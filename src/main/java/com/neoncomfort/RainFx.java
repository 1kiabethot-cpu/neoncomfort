package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Rain FX: цветной «дождь» из частиц вокруг игрока (клиентский, виден только вам):
 * Blood Rain (кровавый), Ember Rain (огненные искры), Neon Rain (цвета меню), Toxic Rain (кислотный), Ash Fall (падающий пепел).
 * Капли падают только там, где над головой открытое небо; у земли разлетаются брызгами. Лёгкий оттенок экрана включается в Rain Tint.
 */
public final class RainFx {
    private static float vis = 0f;

    private static boolean active(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || Modules.RAIN_FX.index <= 0) return false;
        return Modules.RAIN_WHEN.index == 0 || mc.world.isRaining();
    }

    /** 0 Blood, 1 Ember, 2 Neon, 3 Toxic, 4 Ash */
    private static int rgb(int kind, float u, long t) {
        int c = Modules.RAIN_COLOR.index;
        if (c > 0) {
            int n = Theme.colorCount();
            if (c <= n) return Theme.colorRgb(c - 1) & 0xFFFFFF;
            return Theme.rainbowRgb((long) (u * 2000) + t * 20L) & 0xFFFFFF;
        }
        return switch (kind) {
            case 0 -> FxColor.lerp(0x8A0A12, 0xE01824, u);
            case 1 -> FxColor.lerp(0xFF5A10, 0xFFC030, u);
            case 2 -> FxColor.lerp(Theme.accent() & 0xFFFFFF, Theme.accent2() & 0xFFFFFF, u);
            case 3 -> FxColor.lerp(0x4CFF2A, 0xB8FF3A, u);
            default -> FxColor.lerp(0x6F6F6F, 0xB0B0B0, u);
        };
    }

    private static int tick = 0;

    public static void tick(MinecraftClient mc) {
        boolean on = active(mc);
        vis += ((on ? 1f : 0f) - vis) * 0.08f;
        if (!on || mc.isPaused() || Modules.PARTICLE_MODE.index == 2 || mc.player.isSubmergedInWater()) return;
        tick++;
        ClientWorld w = mc.world;
        int kind = Modules.RAIN_FX.index - 1;
        float mult = (Modules.RAIN_AMOUNT.index == 1 ? 0.5f : (Modules.RAIN_AMOUNT.index == 2 ? 1.6f : 1f)) * (Perf.reduced() ? 0.5f : 1f);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean ash = kind == 4;
        float want = (ash ? 4f : 10f) * mult;
        int count = (int) want + (r.nextFloat() < want - (int) want ? 1 : 0);
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (int i = 0; i < count; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0, rad = Math.sqrt(r.nextDouble()) * 14.0;
            double x = px + Math.cos(ang) * rad, z = pz + Math.sin(ang) * rad;
            BlockPos col = BlockPos.ofFloored(x, py + 1.0, z);
            if (!w.isSkyVisible(col)) continue;                                      // под крышей дождя нет
            if (NeonComfort.nearCam(x, py + 3.0, z, 1.2)) continue;
            int ground = w.getTopY(Heightmap.Type.MOTION_BLOCKING, col.getX(), col.getZ());
            double y = Math.max(ground + 2.0, py + 7.0 + r.nextDouble() * 4.0);
            float u = r.nextFloat();
            int color = rgb(kind, u, tick);
            double wind = 0.025;
            if (ash) {                                                               // пепел: медленно, крупно, с дрейфом
                Particle p = mc.particleManager.addParticle(new DustParticleEffect(color, 0.9f), x, y, z, 0, 0, 0);
                if (p != null) {
                    p.setVelocity(wind + (r.nextDouble() - 0.5) * 0.03, -0.10, (r.nextDouble() - 0.5) * 0.03);
                    p.setMaxAge(Math.max(8, (int) ((y - ground) / 0.09)));
                }
                continue;
            }
            int life = Math.max(6, (int) ((y - ground) / 0.55));
            for (int k = 0; k < 2; k++) {                                            // две частицы друг над другом = «штрих» капли
                Particle p = mc.particleManager.addParticle(new DustParticleEffect(color, 0.55f), x, y + k * 0.28, z, 0, 0, 0);
                if (p != null) {
                    p.setVelocity(wind, -0.85, wind * 0.5);
                    p.setMaxAge(life);
                }
            }
            if (r.nextInt(3) == 0) {                                                 // брызги у земли
                double sx = px + (r.nextDouble() - 0.5) * 24.0, sz = pz + (r.nextDouble() - 0.5) * 24.0;
                BlockPos sp = BlockPos.ofFloored(sx, py + 1.0, sz);
                if (w.isSkyVisible(sp)) {
                    int g = w.getTopY(Heightmap.Type.MOTION_BLOCKING, sp.getX(), sp.getZ());
                    Particle p = mc.particleManager.addParticle(new DustParticleEffect(color, 0.45f), sx, g + 0.05, sz, 0, 0, 0);
                    if (p != null) {
                        p.setVelocity((r.nextDouble() - 0.5) * 0.10, 0.09, (r.nextDouble() - 0.5) * 0.10);
                        p.setMaxAge(7);
                    }
                }
            }
        }
    }

    /** лёгкий оттенок экрана под цвет дождя: один градиент */
    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (vis < 0.02f || !Modules.RAIN_TINT.enabled || Modules.RAIN_FX.index <= 0) return;
        int kind = Modules.RAIN_FX.index - 1;
        int top, bot;
        switch (kind) {
            case 0 -> { top = 0x9A0010; bot = 0x3A0008; }
            case 1 -> { top = 0xFF5A10; bot = 0x6A1800; }
            case 2 -> { top = Theme.accent() & 0xFFFFFF; bot = Theme.accent2() & 0xFFFFFF; }
            case 3 -> { top = 0x3AFF2A; bot = 0x104A10; }
            default -> { top = 0x808080; bot = 0x303030; }
        }
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        ctx.fillGradient(0, 0, sw, sh, ((int) (0.10f * vis * 255) << 24) | top, ((int) (0.13f * vis * 255) << 24) | bot);
    }

    private RainFx() {}
}
