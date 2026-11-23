package com.neoncomfort;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Пори року: Spring / Summer / Autumn / Winter (или Cycle: пора меняется каждые 7 игровых дней).
 * Трава и листва перекрашиваются через провайдеры цвета блоков (считаются один раз при сборке чанка, на FPS не влияют),
 * сверху лёгкая цветокоррекция экрана (один градиент) и падающие частицы: лепестки, осенние листья, снег.
 */
public final class Seasons {
    /** 0 = выкл, 1 = весна, 2 = лето, 3 = осень, 4 = зима (то, что сейчас применено к цветам) */
    private static volatile int season = 0;
    private static int applied = -1;

    private static final int GRASS_DEF = 0x79C05A, FOLIAGE_DEF = 0x48B518;
    private static final int[] AUTUMN = { 0xE2701A, 0xD9A21E, 0xB8321A, 0x9C5A1C };

    // ------------------------------------------------------------------ цвета блоков
    public static void init() {
        BlockColorProvider grass = (state, world, pos, tint) -> grass(world, pos);
        ColorProviderRegistry.BLOCK.register(grass, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN,
                Blocks.TALL_GRASS, Blocks.LARGE_FERN);
        BlockColorProvider leaves = (state, world, pos, tint) -> leaves(world, pos, 0);
        ColorProviderRegistry.BLOCK.register(leaves, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
        BlockColorProvider birch = (state, world, pos, tint) -> leaves(world, pos, 1);
        ColorProviderRegistry.BLOCK.register(birch, Blocks.BIRCH_LEAVES);
        BlockColorProvider spruce = (state, world, pos, tint) -> leaves(world, pos, 2);
        ColorProviderRegistry.BLOCK.register(spruce, Blocks.SPRUCE_LEAVES);
    }

    private static int grass(BlockRenderView w, BlockPos p) {
        int base = (w != null && p != null) ? BiomeColors.getGrassColor(w, p) : GRASS_DEF;
        switch (season) {
            case 1: return FxColor.lerp(base, 0x8CE04A, 0.45f);
            case 2: return FxColor.lerp(base, 0x35C21F, 0.30f);
            case 3: return FxColor.lerp(base, 0xB8A236, 0.65f);
            case 4: return FxColor.lerp(base, 0xD8E6EA, 0.75f);
            default: return base;
        }
    }

    /** kind: 0 = обычная листва (по биому), 1 = берёза, 2 = ель */
    private static int leaves(BlockRenderView w, BlockPos p, int kind) {
        int base = kind == 1 ? 0x80A755 : (kind == 2 ? 0x619961
                : ((w != null && p != null) ? BiomeColors.getFoliageColor(w, p) : FOLIAGE_DEF));
        int s = season;
        if (s == 0) return base;
        // «кластер» 8x8 блоков: все листья одного дерева получают один оттенок, соседние деревья разные
        int h = 0;
        if (p != null) {
            h = (p.getX() >> 3) * 73856093 ^ (p.getZ() >> 3) * 19349663;
            h ^= h >>> 13;
            h &= 0x7FFFFFFF;
        }
        switch (s) {
            case 1:                                                    // весна: свежая зелень, часть деревьев цветёт
                if (kind != 2 && h % 5 == 0) return FxColor.lerp(base, 0xF7B6D2, 0.55f);
                return FxColor.lerp(base, 0xA6E67A, 0.30f);
            case 2: return FxColor.lerp(base, 0x2FA81B, 0.30f);
            case 3:                                                    // осень: ель остаётся тёмно-зелёной
                if (kind == 2) return FxColor.lerp(base, 0x4A7A3A, 0.25f);
                return FxColor.lerp(base, AUTUMN[h % AUTUMN.length], 0.85f);
            case 4: return FxColor.lerp(base, 0xBFD6DC, kind == 2 ? 0.45f : 0.65f);
            default: return base;
        }
    }

    // ------------------------------------------------------------------ выбор пор года
    private static int wanted(MinecraftClient mc) {
        int m = Modules.SEASON.index;
        if (m <= 0) return 0;
        if (m <= 4) return m;
        ClientWorld w = mc.world;
        if (w == null) return 0;
        return 1 + (int) ((w.getTimeOfDay() / 24000L / 7L) % 4L);   // Cycle: 7 игровых дней на пору
    }

    public static int current() { return season; }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        int s = wanted(mc);
        if (s != applied) {
            boolean first = applied == -1;
            applied = s;
            season = s;
            if (!(first && s == 0)) mc.worldRenderer.reload();       // один раз: пересобрать чанки с новыми цветами
        }
        if (s != 0 && Modules.SEASON_PARTS.enabled) particles(mc, s);
    }

    // ------------------------------------------------------------------ частицы (раз в тик, мало)
    private static void particles(MinecraftClient mc, int s) {
        ClientWorld w = mc.world;
        if (mc.isPaused() || Modules.PARTICLE_MODE.index == 2 || mc.player.isSubmergedInWater()) return;
        if (s == 2) return;                                            // лето: чистое небо без частиц
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float want = (s == 4 ? 2.2f : 1.4f) * (Perf.reduced() ? 0.5f : 1f);
        if (s == 1 && w.getRainGradient(1f) > 0.6f) want *= 0.5f;
        int count = (int) want + (rnd.nextFloat() < want - (int) want ? 1 : 0);
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0, rad = 3.0 + rnd.nextDouble() * 11.0;
            double x = px + Math.cos(ang) * rad, z = pz + Math.sin(ang) * rad, y = py + 2.0 + rnd.nextDouble() * 7.0;
            BlockPos bp = BlockPos.ofFloored(x, y, z);
            if (!w.getBlockState(bp).isAir() || !w.isSkyVisible(bp)) continue;   // под крышей не падает
            if (NeonComfort.nearCam(x, y, z, 1.5)) continue;
            if (s == 4) {
                mc.particleManager.addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                        (rnd.nextDouble() - 0.5) * 0.02, -0.05, (rnd.nextDouble() - 0.5) * 0.02);
            } else {
                Particle p = mc.particleManager.addParticle(ParticleTypes.CHERRY_LEAVES, x, y, z,
                        0.02 + (rnd.nextDouble() - 0.5) * 0.02, -0.02, (rnd.nextDouble() - 0.5) * 0.02);
                if (p != null && s == 3) {                              // осень: рыже-красные листья
                    float k = rnd.nextFloat();
                    p.setColor(0.95f, 0.45f + 0.35f * k, 0.12f);
                }
            }
        }
    }

    // ------------------------------------------------------------------ лёгкая окраска экрана (1 градиент)
    public static void render(DrawContext ctx, MinecraftClient mc) {
        int s = season;
        if (s == 0 || mc.world == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        switch (s) {
            case 1 -> ctx.fillGradient(0, 0, sw, sh, argb(0.05f, 0xFFC8E6), argb(0.04f, 0xBFFFC8));
            case 2 -> ctx.fillGradient(0, 0, sw, sh, argb(0.05f, 0xFFE9A0), argb(0.01f, 0xFFE9A0));
            case 3 -> ctx.fillGradient(0, 0, sw, sh, argb(0.07f, 0xFF8A2A), argb(0.09f, 0x8A3A10));
            case 4 -> ctx.fillGradient(0, 0, sw, sh, argb(0.08f, 0xBFE4FF), argb(0.05f, 0xFFFFFF));
            default -> { }
        }
    }

    private static int argb(float a, int rgb) {
        return ((int) (Math.max(0f, Math.min(1f, a)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    private Seasons() {}
}
