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
    public static final String VERSION = "1.8.0";
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
                lastHitMs = System.currentTimeMillis();
                killTarget = (LivingEntity) entity;
                killTicks = 0;
                kx = entity.getX(); ky = entity.getY() + entity.getHeight() * 0.6; kz = entity.getZ();
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
        handleKill();
    }

    /** No Hurt Cam: сбрасываем таймер урона, камера не дёргается. */
    private void handleNoHurtCam(ClientPlayerEntity p) {
        if (Modules.NO_HURT_CAM.enabled) p.hurtTime = 0;
    }

    /** время последнего попадания (для Hit Marker) */
    public static volatile long lastHitMs = 0L;

    // Kill Effect: следим за целью, которую только что ударили
    private static LivingEntity killTarget;
    private static int killTicks;
    private static double kx, ky, kz;

    private void handleKill() {
        LivingEntity t = killTarget;
        if (t == null) return;
        if (++killTicks > 80) {
            killTarget = null;
            return;
        }
        if (t.isDead() || t.getHealth() <= 0f) {
            killTarget = null;
            if (Modules.KILL_EFFECT.index > 0) spawnKill(kx, ky, kz);
            return;
        }
        kx = t.getX();
        ky = t.getY() + t.getHeight() * 0.6;
        kz = t.getZ();
    }

    private static void spawnKill(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        int k = Modules.KILL_EFFECT.index;
        ParticleEffect fx;
        int cnt;
        if (k == 1) { fx = ParticleTypes.TOTEM_OF_UNDYING; cnt = 40; }
        else if (k == 2) { fx = ParticleTypes.FIREWORK; cnt = 36; }
        else if (k == 3) { fx = ParticleTypes.SOUL; cnt = 24; }
        else if (k == 4) { fx = ParticleTypes.CRIT; cnt = 40; }
        else if (k == 5) { fx = ParticleTypes.HEART; cnt = 14; }
        else { fx = ParticleTypes.EXPLOSION; cnt = 3; }
        for (int i = 0; i < cnt; i++) {
            double vx = (Math.random() - 0.5) * 0.8, vy = Math.random() * 0.6, vz = (Math.random() - 0.5) * 0.8;
            mc.world.addParticle(fx, x + (Math.random() - 0.5) * 0.5, y + (Math.random() - 0.5) * 0.5,
                    z + (Math.random() - 0.5) * 0.5, vx, vy, vz);
        }
        int cm = Modules.KILL_COLOR.index;
        if (cm > 0) {                                      // цветной слой пыли
            int rgb = Theme.colorRgb(cm - 1);
            for (int i = 0; i < 30; i++) {
                double vx = (Math.random() - 0.5) * 0.7, vy = Math.random() * 0.5, vz = (Math.random() - 0.5) * 0.7;
                mc.world.addParticle(new DustParticleEffect(rgb, 1.4f), x, y, z, vx, vy, vz);
            }
        }
    }

    // количество частиц: Medium, Less, More
    private static final int[] TRAIL_COUNTS = { 5, 2, 9 };
    private static final int[] HIT_COUNTS   = { 14, 6, 28 };

    /** Стили: 0 Dust (цветной), дальше готовые частицы игры. */
    static ParticleEffect effect(int style, int rgb, float scale) {
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

    /** Формы из цветных частиц-пыли: 0 Heart, 1 Star, 2 Ring, 3 Spiral (координаты в плоскости, радиус ~0.5). */
    private static double[][] shape2d(int kind) {
        double[][] pts;
        if (kind == 0) {                                   // Heart
            pts = new double[26][2];
            for (int i = 0; i < pts.length; i++) {
                double t = Math.PI * 2 * i / pts.length;
                double hx = 16 * Math.pow(Math.sin(t), 3);
                double hy = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
                pts[i][0] = hx / 32.0;
                pts[i][1] = (hy + 2) / 32.0;
            }
        } else if (kind == 1) {                            // Star (5 лучей)
            pts = new double[30][2];
            for (int i = 0; i < pts.length; i++) {
                double f = i / 3.0;                        // 10 вершин, по 3 точки на ребро
                int v = (int) Math.floor(f);
                double u = f - v;
                double a1 = Math.PI * 2 * v / 10.0 - Math.PI / 2;
                double a2 = Math.PI * 2 * (v + 1) / 10.0 - Math.PI / 2;
                double r1 = (v % 2 == 0) ? 0.55 : 0.22;
                double r2 = ((v + 1) % 2 == 0) ? 0.55 : 0.22;
                pts[i][0] = (Math.cos(a1) * r1) * (1 - u) + (Math.cos(a2) * r2) * u;
                pts[i][1] = -((Math.sin(a1) * r1) * (1 - u) + (Math.sin(a2) * r2) * u);
            }
        } else if (kind == 2) {                            // Ring
            pts = new double[24][2];
            for (int i = 0; i < pts.length; i++) {
                double t = Math.PI * 2 * i / pts.length;
                pts[i][0] = Math.cos(t) * 0.5;
                pts[i][1] = Math.sin(t) * 0.5;
            }
        } else {                                           // Spiral
            pts = new double[28][2];
            for (int i = 0; i < pts.length; i++) {
                double t = Math.PI * 4 * i / pts.length;
                double r = 0.5 * i / pts.length;
                pts[i][0] = Math.cos(t) * r;
                pts[i][1] = Math.sin(t) * r;
            }
        }
        return pts;
    }

    /** Фигура из цветной пыли лицом к игроку. push = сдвиг к игроку (для удара). */
    private static void spawnShape(MinecraftClient mc, int kind, double cx, double cy, double cz,
                                   int mode, int mode2, double push) {
        if (mc.player == null || mc.world == null) return;
        double yaw = Math.toRadians(mc.player.getYaw());
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);       // вправо от взгляда игрока
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);        // вперёд
        double[][] pts = shape2d(kind);
        for (int i = 0; i < pts.length; i++) {
            double t = i / (double) pts.length;
            int rgb = pickColor(mode, mode2, i * 60L, t);
            double x = cx + rx * pts[i][0] - fx * push;
            double y = cy + pts[i][1];
            double z = cz + rz * pts[i][0] - fz * push;
            mc.world.addParticle(new DustParticleEffect(rgb, 0.8f), x, y, z, 0, 0, 0);
        }
    }

    /**
     * Удар: сам эффект (цвет «Hit FX Color», Original = как в игре) + отдельный слой цветной пыли
     * («Hit Color» / «Hit Color 2»). Например: белая звезда + красная пыль, или наоборот.
     */
    private static void spawnHitParticles(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        int dustMode = Modules.HIT_COLOR.index;          // 0 = без цветной пыли
        int eff = Modules.HIT_EFFECT.index;              // 0 = Dust (отдельного эффекта нет)
        int effMode = Modules.HIT_EFFECT_COLOR.index;    // 0 = оригинальный цвет эффекта
        int glow = Modules.HIT_GLOW.index;
        if (dustMode == 0 && eff == 0 && glow == 0) return;

        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.6, cz = e.getZ();
        int cnt = HIT_COUNTS[Modules.HIT_AMOUNT.index % HIT_COUNTS.length];
        if (eff >= 14) {
            int sm = dustMode != 0 ? dustMode : effMode;
            spawnShape(mc, eff - 14, cx, cy, cz, sm, Modules.HIT_COLOR2.index, 0.3);
        } else {
            for (int i = 0; i < cnt; i++) {
                double ox = (Math.random() - 0.5) * 0.6, oy = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
                double vx = ox * 0.4, vy = oy * 0.4 + 0.05, vz = oz * 0.4;
                if (eff != 0) {
                    if (effMode != 0 && Tint.ok()) {
                        int rgbE = pickColor(effMode, 0, i * 200L, 0.0);
                        Tint.spawn(mc.world, Tint.effect(eff, rgbE, 1.2f), cx + ox, cy + oy, cz + oz, vx, vy, vz);
                    } else {
                        mc.world.addParticle(effect(eff, 0xFFFFFF, 1.2f), cx + ox, cy + oy, cz + oz, vx, vy, vz);
                    }
                }
                if (dustMode != 0) {
                    int rgbD = pickColor(dustMode, Modules.HIT_COLOR2.index, i * 200L, Math.random());
                    mc.world.addParticle(new DustParticleEffect(rgbD, 1.2f), cx + ox, cy + oy, cz + oz, vx, vy, vz);
                }
            }
        }
        if (glow > 0) {
            int gc = cnt / 2 + 1;
            for (int i = 0; i < gc; i++) {
                double ox = (Math.random() - 0.5) * 0.6, oy = (Math.random() - 0.5) * 0.6, oz = (Math.random() - 0.5) * 0.6;
                mc.world.addParticle(glowEffect(glow), cx + ox, cy + oy, cz + oz, ox * 0.3, oy * 0.3 + 0.03, oz * 0.3);
            }
        }
    }

    /** Хвост: сам эффект (цвет «Trail FX Color») + отдельный слой пыли («Trail» / «Trail Color 2»). */
    private void handleTrail(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.world == null) return;
        int dustMode = Modules.TRAIL.index;              // 0 = без цветной пыли
        int eff = Modules.TRAIL_EFFECT.index;
        int effMode = Modules.TRAIL_EFFECT_COLOR.index;  // 0 = оригинальный цвет эффекта
        int glow = Modules.TRAIL_GLOW.index;
        if (dustMode == 0 && eff == 0 && glow == 0) return;

        double dx = p.getX() - p.prevX, dy = p.getY() - p.prevY, dz = p.getZ() - p.prevZ;
        double sp = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (sp < 0.03) return;

        int n = TRAIL_COUNTS[Modules.TRAIL_AMOUNT.index % TRAIL_COUNTS.length];
        if (eff >= 14) {                                   // фигура за спиной раз в 4 тика
            if (p.age % 4 == 0) {
                double yaw = Math.toRadians(p.getYaw());
                int sm = dustMode != 0 ? dustMode : effMode;
                spawnShape(mc, eff - 14, p.getX() + Math.sin(yaw) * 0.9, p.getY() + 0.9,
                        p.getZ() - Math.cos(yaw) * 0.9, sm, Modules.TRAIL_COLOR2.index, 0.0);
            }
            if (glow > 0) {
                for (int i = 0; i < n; i++) {
                    mc.world.addParticle(glowEffect(glow), p.getX() + (Math.random() - 0.5) * 0.5,
                            p.getY() + 0.2 + Math.random() * 0.5, p.getZ() + (Math.random() - 0.5) * 0.5, 0, 0.01, 0);
                }
            }
            return;
        }
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double x = p.prevX + dx * t + (Math.random() - 0.5) * 0.5;
            double y = p.prevY + dy * t + 0.1 + Math.random() * 0.5;
            double z = p.prevZ + dz * t + (Math.random() - 0.5) * 0.5;
            if (eff != 0) {
                if (effMode != 0 && Tint.ok()) {
                    int rgbE = pickColor(effMode, 0, i * 120L, 0.0);
                    Tint.spawn(mc.world, Tint.effect(eff, rgbE, 1.4f), x, y, z, 0, 0, 0);
                } else {
                    mc.world.addParticle(effect(eff, 0xFFFFFF, 1.4f), x, y, z, 0, 0, 0);
                }
            }
            if (dustMode != 0) {
                int rgbD = pickColor(dustMode, Modules.TRAIL_COLOR2.index, i * 120L, t);
                mc.world.addParticle(new DustParticleEffect(rgbD, 1.4f), x, y, z, 0, 0, 0);
            }
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
