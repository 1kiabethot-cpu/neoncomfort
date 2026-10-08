package com.neoncomfort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class NeonComfort implements ClientModInitializer {
    public static final String VERSION = "1.5.0";
    private static final String CATEGORY = "category.neoncomfort";

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;

    private boolean zooming = false;
    private int savedFov = 70;
    private boolean fullbrightWasOn = false;

    // Hit Sound: 0 = OFF, далее по порядку из Modules.HIT_SOUND
    private static final String[] HIT_IDS = {
            null, "entity.experience_orb.pickup", "block.note_block.bell",
            "block.note_block.pling", "entity.arrow.hit_player", "entity.player.levelup",
            "block.amethyst_block.chime", "entity.player.attack.crit" };
    private static final float[] HIT_PITCH = { 1f, 1.0f, 1.2f, 1.4f, 1.0f, 1.2f, 1.3f, 1.0f };
    private static final float[] HIT_VOL   = { 1f, 0.9f, 1.0f, 1.0f, 1.0f, 0.6f, 1.0f, 0.9f };
    private static final float[] VOL_MULT  = { 0.7f, 0.35f, 1.0f };   // Normal, Quiet, Loud

    @Override
    public void onInitializeClient() {
        Config.load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neoncomfort.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neoncomfort.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));

        // Звук при ударе по живому существу (только на твоём клиенте)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof LivingEntity) {
                playHitSound();
                spawnHitParticles(entity);
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // Кнопка в меню паузы — удобно на телефоне, где нет RShift
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("NeonComfort"),
                                        b -> client.setScreen(new MenuScreen()))
                                .dimensions(6, 6, 100, 20).build());
            }
        });
    }

    private void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new MenuScreen());
        }

        handleZoom(mc);
        handleFullbright(p);
        handleAutoSprint(mc, p);
        handleWeather(mc);
        handleTrail(mc, p);
        handleNoHurtCam(p);
    }

    // количество частиц: Medium, Less, More
    private static final int[] TRAIL_COUNTS = { 5, 2, 9 };
    private static final int[] HIT_COUNTS   = { 14, 6, 28 };

    /** Стили: 0 Dust (цветной), дальше готовые частицы игры. */
    private static ParticleEffect effect(int style, int rgb, float scale) {
        switch (style) {
            case 1: return ParticleTypes.CRIT;
            case 2: return ParticleTypes.ENCHANTED_HIT;
            case 3: return ParticleTypes.END_ROD;
            case 4: return ParticleTypes.FLAME;
            case 5: return ParticleTypes.SOUL_FIRE_FLAME;
            case 6: return ParticleTypes.ELECTRIC_SPARK;
            case 7: return ParticleTypes.HEART;
            case 8: return ParticleTypes.NOTE;
            case 9: return ParticleTypes.SNOWFLAKE;
            case 10: return ParticleTypes.GLOW;
            case 11: return ParticleTypes.TOTEM_OF_UNDYING;
            case 12: return ParticleTypes.ENCHANT;
            case 13: return ParticleTypes.HAPPY_VILLAGER;
            default: return new DustParticleEffect(rgb, scale);
        }
    }

    /** Светящиеся частицы-добавка: 1 Soft, 2 Bright, 3 Spark. */
    private static ParticleEffect glowEffect(int g) {
        if (g == 2) return ParticleTypes.END_ROD;
        if (g == 3) return ParticleTypes.ELECTRIC_SPARK;
        return ParticleTypes.GLOW;
    }

    private static int mixColor(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Цвет частицы: mode = основной цвет (1..N, N+1 = Rainbow), mode2 = второй цвет для плавного перехода. */
    private static int pickColor(int mode, int mode2, long shift, double t) {
        if (mode <= 0) return 0xFFFFFF;
        if (mode == Theme.colorCount() + 1) return Theme.rainbowRgb(shift);
        int c1 = Theme.colorRgb(mode - 1);
        if (mode2 > 0) return mixColor(c1, Theme.colorRgb(mode2 - 1), t);
        return c1;
    }

    public static void playHitSound() {
        int i = Modules.HIT_SOUND.index;
        if (i <= 0 || i >= HIT_IDS.length) return;
        SoundEvent ev = SoundEvent.of(Identifier.ofVanilla(HIT_IDS[i]));
        float vol = HIT_VOL[i] * VOL_MULT[Modules.HIT_VOLUME.index % VOL_MULT.length];
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(ev, HIT_PITCH[i], vol));
    }

    /** Частицы в месте удара: эффект, цвет (можно смешать два) и светящаяся добавка. */
    private static void spawnHitParticles(Entity e) {
        int mode = Modules.HIT_COLOR.index;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mode == 0 || mc.world == null) return;
        int eff = Modules.HIT_EFFECT.index, glow = Modules.HIT_GLOW.index;
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.6, cz = e.getZ();
        int cnt = HIT_COUNTS[Modules.HIT_AMOUNT.index % HIT_COUNTS.length];
        for (int i = 0; i < cnt; i++) {
            int rgb = pickColor(mode, Modules.HIT_COLOR2.index, i * 200L, Math.random());
            ParticleEffect fx = effect(eff, rgb, 1.2f);
            double ox = (Math.random() - 0.5) * 0.6, oy = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
            mc.world.addParticle(fx, cx + ox, cy + oy, cz + oz, ox * 0.4, oy * 0.4 + 0.05, oz * 0.4);
        }
        if (glow > 0) {
            int gc = cnt / 2 + 1;
            for (int i = 0; i < gc; i++) {
                double ox = (Math.random() - 0.5) * 0.6, oy = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
                mc.world.addParticle(glowEffect(glow), cx + ox, cy + oy, cz + oz, ox * 0.3, oy * 0.3 + 0.03, oz * 0.3);
            }
        }
    }

    /** Хвост у ног: эффект, цвет (можно смешать два) и светящаяся добавка. */
    private void handleTrail(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.TRAIL.index;
        if (mode == 0 || mc.world == null) return;
        double dx = p.getX() - p.prevX, dy = p.getY() - p.prevY, dz = p.getZ() - p.prevZ;
        double sp = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (sp < 0.03) return;

        int eff = Modules.TRAIL_EFFECT.index, glow = Modules.TRAIL_GLOW.index;
        int n = TRAIL_COUNTS[Modules.TRAIL_AMOUNT.index % TRAIL_COUNTS.length];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            int rgb = pickColor(mode, Modules.TRAIL_COLOR2.index, i * 120L, t);
            double x = p.prevX + dx * t + (Math.random() - 0.5) * 0.5;
            double y = p.prevY + dy * t + 0.1 + Math.random() * 0.5;
            double z = p.prevZ + dz * t + (Math.random() - 0.5) * 0.5;
            mc.world.addParticle(effect(eff, rgb, 1.4f), x, y, z, 0, 0, 0);
            if (glow > 0) mc.world.addParticle(glowEffect(glow), x, y + 0.1, z, 0, 0.01, 0);
        }
    }

    /** Погода только на клиенте: видишь только ты, сервер не меняется. */
    private void handleWeather(MinecraftClient mc) {
        ClientWorld w = mc.world;
        int mode = Modules.WEATHER.index;
        if (w == null || mode == 0) return;
        w.setRainGradient(mode == 2 || mode == 3 ? 1f : 0f);
        w.setThunderGradient(mode == 3 ? 1f : 0f);
    }

    private void handleZoom(MinecraftClient mc) {
        boolean want = Modules.ZOOM.enabled && zoomKey.isPressed() && mc.currentScreen == null;
        if (want && !zooming) {
            savedFov = mc.options.getFov().getValue();
            mc.options.getFov().setValue(30);
            zooming = true;
        } else if (!want && zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private void handleFullbright(ClientPlayerEntity p) {
        if (Modules.FULLBRIGHT.enabled) {
            p.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            fullbrightWasOn = true;
        } else if (fullbrightWasOn) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            fullbrightWasOn = false;
        }
    }

    private void handleAutoSprint(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.AUTOSPRINT.enabled) return;
        if (mc.options.forwardKey.isPressed()
                && !p.isSneaking()
                && !p.horizontalCollision
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
