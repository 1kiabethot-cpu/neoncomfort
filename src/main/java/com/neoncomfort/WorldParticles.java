package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * 3D-визуал мира (настоящие частицы в мире, а не наложение на экран):
 *  - World Particles: светлячки, снег, сакура, жар, звёзды, магия, неоновый пыль вокруг игрока;
 *  - Pulse Wave: расходящиеся по земле кольца-волны от игрока;
 *  - Fly Visuals: ленты / спираль / ветер при полёте (элитры или creative-полёт).
 * Используются только частицы, которые уже есть в моде, поэтому нагрузка маленькая.
 */
public final class WorldParticles {
    private static final Random R = new Random();
    private static int t = 0;

    /** волны: {x, y, z, age} */
    private static final ArrayList<double[]> WAVES = new ArrayList<>();
    private static int waveCd = 0;

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            WAVES.clear();
            return;
        }
        t++;
        ClientPlayerEntity p = mc.player;
        ambient(mc, p);
        pulseWave(mc, p);
        flyFx(mc, p);
    }

    // ===================== World Particles =====================
    private static void ambient(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.WORLD_PARTS.index;
        if (mode == 0) return;
        int amount = Modules.WORLD_PARTS_AMOUNT.index;           // 0 Normal, 1 Few, 2 Many
        int n = amount == 1 ? 1 : (amount == 2 ? 4 : 2);
        if (amount == 1 && (t & 1) == 1) return;                 // Few: через тик
        boolean fromSky = mode == 2 || mode == 3;                // снег и сакура падают сверху, только под открытым небом

        for (int i = 0; i < n; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            double rad = 2.5 + R.nextDouble() * 11.0;
            double x = p.getX() + Math.cos(ang) * rad;
            double z = p.getZ() + Math.sin(ang) * rad;
            double y = fromSky ? p.getY() + 4.0 + R.nextDouble() * 6.0 : p.getY() - 1.0 + R.nextDouble() * 5.0;
            BlockPos bp = BlockPos.ofFloored(x, y, z);
            if (!mc.world.getBlockState(bp).isAir()) continue;
            if (fromSky && !mc.world.isSkyVisible(bp)) continue;
            if (NeonComfort.nearCam(x, y, z, 1.5)) continue;
            spawnAmbient(mc, mode, x, y, z);
        }
    }

    private static void spawnAmbient(MinecraftClient mc, int mode, double x, double y, double z) {
        float u = R.nextFloat();
        boolean auto = Modules.WORLD_PARTS_COLOR.index == 0;
        int custom = auto ? 0 : FxColor.pick(Modules.WORLD_PARTS_COLOR, Modules.WORLD_PARTS_COLOR2, u, t * 40L);
        switch (mode) {
            case 1 -> {                                           // Fireflies: тёпло-зелёные огоньки, медленно плавают
                int rgb = auto ? FxColor.lerp(0xC8FF5A, 0xFFF4B0, u) : custom;
                mc.world.addParticle(new DustParticleEffect(rgb, 0.7f), x, y, z,
                        (R.nextDouble() - 0.5) * 0.01, (R.nextDouble() - 0.4) * 0.01, (R.nextDouble() - 0.5) * 0.01);
                if (R.nextInt(6) == 0) mc.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.004, 0);
            }
            case 2 -> mc.world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                    (R.nextDouble() - 0.5) * 0.02, -0.03, (R.nextDouble() - 0.5) * 0.02);
            case 3 -> {                                           // Sakura: розовые «лепестки» падают и качаются
                int rgb = auto ? FxColor.lerp(0xFFA6C9, 0xFFD6E6, u) : custom;
                mc.world.addParticle(new DustParticleEffect(rgb, 1.0f), x, y, z,
                        Math.sin(t * 0.07 + x) * 0.03, -0.035, Math.cos(t * 0.07 + z) * 0.03);
            }
            case 4 -> {                                           // Embers: искры жара поднимаются вверх
                int rgb = auto ? FxColor.lerp(0xFF6A1A, 0xFFC040, u) : custom;
                mc.world.addParticle(new DustParticleEffect(rgb, 0.8f), x, y, z,
                        (R.nextDouble() - 0.5) * 0.02, 0.04, (R.nextDouble() - 0.5) * 0.02);
                if (R.nextInt(8) == 0) mc.world.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
            }
            case 5 -> mc.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);   // Stars: неподвижные блёстки
            case 6 -> {                                           // Magic: фиолетово-бирюзовая пыль + руны
                int rgb = auto ? FxColor.lerp(0xB07CFF, 0x4CC9FF, u) : custom;
                mc.world.addParticle(new DustParticleEffect(rgb, 0.9f), x, y, z, 0, 0.015, 0);
                if (R.nextInt(5) == 0) mc.world.addParticle(ParticleTypes.ENCHANT, x, y + 0.5, z, 0, 0.1, 0);
            }
            default -> {                                          // Neon Dust: цвет меню / выбранный цвет
                int rgb = auto ? FxColor.pick(Modules.MENU_COLOR, Modules.WORLD_PARTS_COLOR2, u, t * 40L) : custom;
                mc.world.addParticle(new DustParticleEffect(rgb, 0.9f), x, y, z,
                        (R.nextDouble() - 0.5) * 0.01, 0.01, (R.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    // ===================== Pulse Wave =====================
    private static void pulseWave(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.PULSE_WAVE.index;                     // 1 Slow, 2 Fast, 3 Double
        if (mode == 0) {
            WAVES.clear();
            waveCd = 0;
            return;
        }
        int period = mode == 1 ? 50 : 26;
        if (--waveCd <= 0 && p.isOnGround()) {
            WAVES.add(new double[] { p.getX(), p.getY() + 0.06, p.getZ(), 0 });
            if (mode == 3) WAVES.add(new double[] { p.getX(), p.getY() + 0.06, p.getZ(), -5 });   // вторая волна с отставанием
            waveCd = period;
        }
        Iterator<double[]> it = WAVES.iterator();
        while (it.hasNext()) {
            double[] w = it.next();
            w[3] += 1;
            if (w[3] < 1) continue;
            double r = 0.6 + w[3] * 0.36;
            if (r > 8.5) {
                it.remove();
                continue;
            }
            int n = Math.min(44, 10 + (int) (r * 5));
            float fade = (float) (1.0 - r / 8.5);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                double x = w[0] + Math.cos(a) * r, z = w[2] + Math.sin(a) * r;
                if (NeonComfort.nearCam(x, w[1], z, 1.5)) continue;
                float u = 0.5f + 0.5f * (float) Math.sin(a * 2 + w[3] * 0.3);
                int rgb = FxColor.pick(Modules.PULSE_COLOR, Modules.PULSE_COLOR2, u, (long) (w[3] * 70) + i * 15L);
                mc.world.addParticle(new DustParticleEffect(rgb, 0.7f + 0.5f * fade), x, w[1], z, 0, 0.005, 0);
                if (i % 9 == 0) mc.world.addParticle(ParticleTypes.END_ROD, x, w[1], z, 0, 0.01, 0);
            }
        }
    }

    // ===================== Fly Visuals =====================
    private static void flyFx(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.FLY_FX.index;                         // 1 Ribbon, 2 Spiral, 3 Wind
        if (mode == 0) return;
        boolean flying = p.isFallFlying() || p.getAbilities().flying;
        if (!flying) return;
        Vec3d v = p.getVelocity();
        double px = p.getX(), py = p.getY(), pz = p.getZ();

        switch (mode) {
            case 1 -> {                                           // Ribbon: две ленты за плечами
                double hl = Math.sqrt(v.x * v.x + v.z * v.z);
                double sx = hl > 0.01 ? -v.z / hl : 1.0, sz = hl > 0.01 ? v.x / hl : 0.0;
                for (int side = -1; side <= 1; side += 2) {
                    double x = px + sx * 0.45 * side - v.x * 0.6;
                    double y = py + 0.9 - v.y * 0.6;
                    double z = pz + sz * 0.45 * side - v.z * 0.6;
                    if (NeonComfort.nearCam(x, y, z, 1.2)) continue;
                    float u = 0.5f + 0.5f * (float) Math.sin(t * 0.2 + side);
                    int rgb = FxColor.pick(Modules.FLY_COLOR, Modules.FLY_COLOR2, u, t * 30L);
                    mc.world.addParticle(new DustParticleEffect(rgb, 1.0f), x, y, z, 0, 0, 0);
                    if ((t + side) % 6 == 0) mc.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
                }
            }
            case 2 -> {                                           // Spiral: двойная спираль вокруг тела
                for (int k = 0; k < 2; k++) {
                    double a = t * 0.45 + k * Math.PI;
                    double h = ((t * 0.08 + k * 0.5) % 1.0) * 1.8;
                    double x = px + Math.cos(a) * 0.9 - v.x * 0.4;
                    double y = py + 0.1 + h;
                    double z = pz + Math.sin(a) * 0.9 - v.z * 0.4;
                    if (NeonComfort.nearCam(x, y, z, 1.2)) continue;
                    int rgb = FxColor.pick(Modules.FLY_COLOR, Modules.FLY_COLOR2, (float) (h / 1.8), t * 30L);
                    mc.world.addParticle(new DustParticleEffect(rgb, 1.0f), x, y, z, 0, 0, 0);
                }
            }
            default -> {                                          // Wind: белые росчерки ветра вокруг
                for (int k = 0; k < 2; k++) {
                    double x = px + (R.nextDouble() - 0.5) * 3.0 + v.x * 2.0;
                    double y = py + R.nextDouble() * 2.0 - 0.3 + v.y * 2.0;
                    double z = pz + (R.nextDouble() - 0.5) * 3.0 + v.z * 2.0;
                    if (NeonComfort.nearCam(x, y, z, 1.2)) continue;
                    ParticleEffect fx = ParticleTypes.END_ROD;
                    mc.world.addParticle(fx, x, y, z, -v.x * 0.25, -v.y * 0.25, -v.z * 0.25);
                }
            }
        }
    }

    private WorldParticles() {}
}
