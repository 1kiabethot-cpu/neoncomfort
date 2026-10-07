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
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class NeonComfort implements ClientModInitializer {
    public static final String VERSION = "1.3.0";
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

    public static void playHitSound() {
        int i = Modules.HIT_SOUND.index;
        if (i <= 0 || i >= HIT_IDS.length) return;
        SoundEvent ev = SoundEvent.of(Identifier.ofVanilla(HIT_IDS[i]));
        float vol = HIT_VOL[i] * VOL_MULT[Modules.HIT_VOLUME.index % VOL_MULT.length];
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(ev, HIT_PITCH[i], vol));
    }

    /** Цветные частицы в месте удара (цвет как у хвоста). */
    private static void spawnHitParticles(Entity e) {
        int mode = Modules.HIT_COLOR.index;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mode == 0 || mc.world == null) return;
        boolean rainbow = (mode == Theme.colorCount() + 1);
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.6, cz = e.getZ();
        for (int i = 0; i < 14; i++) {
            int rgb = rainbow ? Theme.rainbowRgb(i * 200L) : Theme.colorRgb(mode - 1);
            DustParticleEffect fx = new DustParticleEffect(rgb, 1.2f);
            double ox = (Math.random() - 0.5) * 0.6, oy = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
            mc.world.addParticle(fx, cx + ox, cy + oy, cz + oz, ox * 0.4, oy * 0.4 + 0.05, oz * 0.4);
        }
    }

    /** No Hurt Cam: сбрасываем таймер урона, камера не дёргается. */
    private void handleNoHurtCam(ClientPlayerEntity p) {
        if (Modules.NO_HURT_CAM.enabled) p.hurtTime = 0;
    }

    /** Цветной хвост у ног: виден и от первого лица (если глянуть вниз/назад), и в F5. */
    private void handleTrail(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.TRAIL.index;
        if (mode == 0 || mc.world == null) return;
        double dx = p.getX() - p.prevX, dy = p.getY() - p.prevY, dz = p.getZ() - p.prevZ;
        double sp = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (sp < 0.03) return;

        boolean rainbow = (mode == Theme.colorCount() + 1);
        int n = 5;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            int rgb = rainbow ? Theme.rainbowRgb(i * 120L) : Theme.colorRgb(mode - 1);
            DustParticleEffect fx = new DustParticleEffect(rgb, 1.4f);
            double x = p.prevX + dx * t + (Math.random() - 0.5) * 0.5;
            double y = p.prevY + dy * t + 0.1 + Math.random() * 0.5;
            double z = p.prevZ + dz * t + (Math.random() - 0.5) * 0.5;
            mc.world.addParticle(fx, x, y, z, 0, 0, 0);
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
