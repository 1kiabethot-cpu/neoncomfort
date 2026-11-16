package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * World FX + Soft Shader.
 * Все эффекты рисуются несколькими полупрозрачными прямоугольниками поверх кадра (как Twilight/Fog),
 * поэтому видеокарте почти ничего не стоят: никаких пост-шейдеров, FBO и лишних проходов рендера.
 * Работает и на Pojav / слабых ПК.
 */
public final class WorldFx {
    // сила: Medium / Soft / Strong
    private static final float[] POWER = { 1.0f, 0.55f, 1.5f };

    // плавное появление луны/планеты (меняется раз в тик, рисуется каждый кадр)
    private static float moonVis = 0f;
    private static float planetVis = 0f;
    private static float sunVis = 0f;

    // направление «планеты» в режиме Space (фиксированная точка неба)
    private static final double PX = 0.55, PY = 0.45, PZ = -0.70;

    // ===================== тик: проверка, не закрыта ли луна горой/блоками =====================
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        int mode = Modules.WORLD_FX.index;
        boolean needMoon = mode == 1;
        boolean needPlanet = mode == 3;
        boolean needSun = mode == 6;
        Camera cam = mc.gameRenderer.getCamera();
        if (needMoon) {
            Vec3d d = moonDir(mc.world);
            moonVis = approach(moonVis, d.y > -0.02 && skyVisible(mc, cam.getPos(), d) ? 1f : 0f);
        } else moonVis = 0f;
        if (needSun) {
            Vec3d m = moonDir(mc.world);
            Vec3d d = new Vec3d(-m.x, -m.y, -m.z);
            sunVis = approach(sunVis, d.y > -0.02 && skyVisible(mc, cam.getPos(), d) ? 1f : 0f);
        } else sunVis = 0f;
        if (needPlanet) {
            Vec3d d = new Vec3d(PX, PY, PZ).normalize();
            planetVis = approach(planetVis, skyVisible(mc, cam.getPos(), d) ? 1f : 0f);
        } else planetVis = 0f;
    }

    private static float approach(float v, float target) {
        return v + (target - v) * 0.12f;
    }

    /** один луч за тик на объект: открыто ли небо в этом направлении */
    private static boolean skyVisible(MinecraftClient mc, Vec3d from, Vec3d dir) {
        Vec3d to = from.add(dir.multiply(160.0));
        RaycastContext rc = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player);
        return mc.world.raycast(rc).getType() == HitResult.Type.MISS;
    }

    // ===================== геометрия неба =====================
    /** направление на луну (вектор из центра мира), по формуле вращения неба из ванили */
    private static Vec3d moonDir(ClientWorld w) {
        double th = Math.toRadians(w.getSkyAngle(0f) * 360.0);
        return new Vec3d(Math.sin(th), -Math.cos(th), 0.0);
    }

    /** 0 = день, 1 = глубокая ночь (по высоте луны над горизонтом) */
    private static float nightFactor(ClientWorld w) {
        double moonY = -Math.cos(Math.toRadians(w.getSkyAngle(0f) * 360.0));
        return clamp((float) ((moonY + 0.08) / 0.30));
    }

    /** проекция направления на экран: {x, y, радиус(для объекта углового размера rad), видимость} или null */
    private static float[] project(MinecraftClient mc, Vec3d d, double angularRadius) {
        Camera cam = mc.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cp = Math.cos(pitch);
        double fx = -Math.sin(yaw) * cp, fy = -Math.sin(pitch), fz = Math.cos(yaw) * cp;
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double df = d.x * fx + d.y * fy + d.z * fz;
        if (df < 0.08) return null;
        double dr = d.x * rx + d.y * ry + d.z * rz;
        double du = d.x * ux + d.y * uy + d.z * uz;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        double tanH = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) sw / sh;
        float sx = (float) (sw / 2.0 + (dr / df) / (tanH * aspect) * sw / 2.0);
        float sy = (float) (sh / 2.0 - (du / df) / tanH * sh / 2.0);
        float r = (float) (Math.tan(angularRadius) / tanH * sh / 2.0);
        return new float[] { sx, sy, Math.min(r, 90f) };
    }

    // ===================== рисование =====================
    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();
        float pw = POWER[Modules.WORLD_FX_POWER.index];
        float night = nightFactor(mc.world);

        switch (Modules.WORLD_FX.index) {
            case 1 -> bloodMoon(ctx, mc, sw, sh, now, pw, night);
            case 2 -> ocean(ctx, mc, sw, sh, now, pw, night);
            case 3 -> space(ctx, mc, sw, sh, now, pw, night);
            case 4 -> aurora(ctx, sw, sh, now, pw, night);
            case 5 -> dreamy(ctx, sw, sh, now, pw);
            case 6 -> synthwave(ctx, mc, sw, sh, now, pw);
            case 7 -> rainbow(ctx, sw, sh, now, pw);
            case 8 -> pulse(ctx, sw, sh, now, pw);
            case 9 -> inferno(ctx, sw, sh, now, pw);
            default -> { }
        }
        if (Modules.SOFT_SHADER.index > 0) softShader(ctx, sw, sh, Modules.SOFT_SHADER.index);
    }

    /** Кровавая луна: красное небо-дымка, пульсирующая красная луна с ореолом, красная виньетка. Только ночью. */
    private static void bloodMoon(DrawContext ctx, MinecraftClient mc, int sw, int sh, long now, float pw, float night) {
        if (night <= 0.01f) return;
        float pulse = 0.85f + 0.15f * (float) Math.sin(now / 900.0);
        float a = night * pw * pulse;
        // красное зарево: сверху тёмно-красное, к горизонту ярче
        ctx.fillGradient(0, 0, sw, sh / 2, col(0.30f * a, 0x5A0000), col(0.14f * a, 0xB01010));
        ctx.fillGradient(0, sh / 2, sw, sh, col(0.14f * a, 0xB01010), col(0.28f * a, 0x3A0000));
        vignette(ctx, sw, sh, 0.50f * a, 0x7A0000);

        // сама луна
        if (moonVis > 0.02f) {
            float[] p = project(mc, moonDir(mc.world), Math.toRadians(5.7 * 1.45));
            if (p != null) {
                float v = moonVis * night;
                for (int g = 4; g >= 1; g--) disc(ctx, p[0], p[1], p[2] * (1f + g * 0.32f), col(0.07f * v * pw, 0xFF1A1A));
                disc(ctx, p[0], p[1], p[2], col(0.95f * v, 0xC41212));
                disc(ctx, p[0] - p[2] * 0.18f, p[1] - p[2] * 0.12f, p[2] * 0.72f, col(0.55f * v, 0xE63030));
                disc(ctx, p[0] + p[2] * 0.30f, p[1] + p[2] * 0.25f, p[2] * 0.20f, col(0.45f * v, 0x7A0808)); // «кратеры»
                disc(ctx, p[0] - p[2] * 0.35f, p[1] + p[2] * 0.30f, p[2] * 0.14f, col(0.40f * v, 0x7A0808));
            }
        }
    }

    /** Морской режим: бирюзовая толща, мерцающие лучи света; под водой сильнее. */
    private static void ocean(DrawContext ctx, MinecraftClient mc, int sw, int sh, long now, float pw, float night) {
        boolean under = mc.player.isSubmergedInWater();
        float a = pw * (under ? 1.5f : 0.8f) * (1f - 0.35f * night);
        ctx.fillGradient(0, 0, sw, sh, col(0.05f * a, 0x2FD6E8), col(0.30f * a, 0x0A4F8C));
        // лучи света (caustics): 6 наклонённых полос, плавно качаются
        for (int i = 0; i < 6; i++) {
            double ph = now / 2600.0 + i * 1.7;
            int x = (int) (sw * (0.08 + 0.16 * i) + Math.sin(ph) * sw * 0.04);
            int wd = (int) (sw * (0.035 + 0.02 * Math.sin(ph * 1.3 + i)));
            float la = (0.05f + 0.04f * (float) Math.sin(ph * 1.9)) * a * (under ? 1.4f : 1f);
            ctx.fillGradient(x, 0, x + wd, sh * 3 / 4, col(Math.max(0f, la), 0xBFFFF8), col(0f, 0xBFFFF8));
        }
        vignette(ctx, sw, sh, 0.22f * a, 0x03203A);
    }

    /** Космос: фиолетово-синяя туманность, звёзды-вспышки и планета с кольцом. Ночью (днём слабо). */
    private static void space(DrawContext ctx, MinecraftClient mc, int sw, int sh, long now, float pw, float night) {
        float a = pw * (0.30f + 0.70f * night);
        ctx.fillGradient(0, 0, sw, sh / 2, col(0.30f * a, 0x0B0830), col(0.12f * a, 0x4B1C8C));
        ctx.fillGradient(0, sh / 2, sw, sh, col(0.12f * a, 0x4B1C8C), col(0.22f * a, 0x120A3A));
        // две мягкие туманности
        float s = (float) Math.sin(now / 5000.0);
        ctx.fillGradient((int) (sw * 0.10f + s * 6), (int) (sh * 0.05f), (int) (sw * 0.55f + s * 6), (int) (sh * 0.45f),
                col(0.14f * a, 0xD05CFF), col(0f, 0xD05CFF));
        ctx.fillGradient((int) (sw * 0.50f - s * 6), (int) (sh * 0.15f), (int) (sw * 0.95f - s * 6), (int) (sh * 0.55f),
                col(0.12f * a, 0x4CC9FF), col(0f, 0x4CC9FF));
        // мерцающие звёзды (детерминированные, без Random): 40 штук
        for (int i = 0; i < 40; i++) {
            float fx = frac(i * 0.6180339f), fy = frac(i * 0.7548776f) * 0.7f;
            float tw = 0.4f + 0.6f * (float) Math.abs(Math.sin(now / 700.0 + i * 1.9));
            int x = (int) (fx * sw), y = (int) (fy * sh);
            ctx.fill(x, y, x + 1 + (i % 5 == 0 ? 1 : 0), y + 1 + (i % 5 == 0 ? 1 : 0), col(0.8f * tw * a, 0xFFFFFF));
        }
        // планета с кольцом
        if (planetVis > 0.02f) {
            float[] p = project(mc, new Vec3d(PX, PY, PZ).normalize(), Math.toRadians(9.0));
            if (p != null) {
                float v = planetVis * Math.max(0.35f, night);
                float r = p[2];
                for (int g = 3; g >= 1; g--) disc(ctx, p[0], p[1], r * (1f + g * 0.25f), col(0.05f * v, 0xB07CFF));
                disc(ctx, p[0], p[1], r, col(0.95f * v, 0x5A3FB8));
                disc(ctx, p[0] - r * 0.25f, p[1] - r * 0.25f, r * 0.7f, col(0.55f * v, 0x8E6BFF));
                int rw = (int) (r * 1.9f);
                ctx.fill((int) p[0] - rw, (int) p[1] - 1, (int) p[0] + rw, (int) p[1] + 1, col(0.7f * v, 0xE6D2FF));
                ctx.fill((int) p[0] - (int) (rw * 0.8f), (int) p[1] + 2, (int) p[0] + (int) (rw * 0.8f), (int) p[1] + 3, col(0.4f * v, 0xE6D2FF));
            }
        }
        vignette(ctx, sw, sh, 0.25f * a, 0x07041C);
    }

    /** Северное сияние: переливающиеся зелёно-бирюзово-фиолетовые «занавесы» в верхней части. Ночью. */
    private static void aurora(DrawContext ctx, int sw, int sh, long now, float pw, float night) {
        if (night <= 0.02f) return;
        float a = night * pw;
        int n = 36, wd = sw / n + 1;
        for (int i = 0; i < n; i++) {
            double ph = now / 2200.0 + i * 0.35;
            float hgt = sh * (0.20f + 0.14f * (float) Math.sin(ph) + 0.06f * (float) Math.sin(ph * 2.3 + i));
            float k = (float) (0.5 + 0.5 * Math.sin(now / 3500.0 + i * 0.22));
            int rgb = lerp(0x35FFA0, 0x9D5CFF, k);
            float la = (0.14f + 0.08f * (float) Math.sin(ph * 1.5)) * a;
            int x = i * (sw / n);
            ctx.fillGradient(x, 0, x + wd, (int) hgt, col(0f, rgb), col(Math.max(0f, la), rgb));
            ctx.fillGradient(x, (int) hgt, x + wd, (int) (hgt + sh * 0.12f), col(Math.max(0f, la), rgb), col(0f, rgb));
        }
        ctx.fillGradient(0, 0, sw, sh, col(0.10f * a, 0x021A22), col(0.04f * a, 0x021A22));
    }

    /** Dreamy: тёплый розово-персиково-фиолетовый «закат» на весь день. */
    private static void dreamy(DrawContext ctx, int sw, int sh, long now, float pw) {
        float s = 0.9f + 0.1f * (float) Math.sin(now / 3000.0);
        float a = pw * s;
        ctx.fillGradient(0, 0, sw, sh / 2, col(0.20f * a, 0xB06CFF), col(0.20f * a, 0xFF8FB8));
        ctx.fillGradient(0, sh / 2, sw, sh, col(0.20f * a, 0xFF8FB8), col(0.22f * a, 0xFFC27A));
        vignette(ctx, sw, sh, 0.18f * a, 0x5A1E6E);
    }


    /** Synthwave: фиолетово-розовое небо, ретро-солнце с полосами по направлению настоящего солнца, бегущие линии сетки внизу. */
    private static void synthwave(DrawContext ctx, MinecraftClient mc, int sw, int sh, long now, float pw) {
        float a = pw;
        ctx.fillGradient(0, 0, sw, sh / 2, col(0.32f * a, 0x1A0640), col(0.22f * a, 0xFF2E93));
        ctx.fillGradient(0, sh / 2, sw, sh, col(0.22f * a, 0xFF2E93), col(0.30f * a, 0x12032B));
        if (sunVis > 0.02f) {
            Vec3d m = moonDir(mc.world);
            float[] p = project(mc, new Vec3d(-m.x, -m.y, -m.z), Math.toRadians(5.7 * 1.6));
            if (p != null) {
                float v = sunVis * Math.min(1f, a);
                int ri = Math.max(2, Math.round(p[2]));
                for (int g = 3; g >= 1; g--) disc(ctx, p[0], p[1], ri * (1f + g * 0.28f), col(0.06f * v, 0xFF4FB8));
                for (int dy = -ri; dy <= ri; dy++) {
                    float k = (dy + ri) / (2f * ri);                      // 0 = верх диска, 1 = низ
                    if (k > 0.45f && ((dy + ri) % 6) < (int) (k * 5f)) continue;   // ретро-вырезы к низу
                    int half = (int) Math.sqrt((double) ri * ri - (double) dy * dy);
                    ctx.fill((int) p[0] - half, (int) p[1] + dy, (int) p[0] + half, (int) p[1] + dy + 1,
                            col(0.95f * v, lerp(0xFFE45C, 0xFF2E93, k)));
                }
            }
        }
        int base = sh * 2 / 3;
        for (int i = 0; i < 7; i++) {                                     // линии «сетки» едут к зрителю
            float ph = (float) (((now / 1800.0) + i / 7.0) % 1.0);
            float kk = ph * ph;
            int y = base + (int) (kk * (sh - base));
            ctx.fill(0, y, sw, y + 1, col((0.05f + 0.16f * kk) * a, 0xFF5CD6));
        }
        vignette(ctx, sw, sh, 0.30f * a, 0x14002E);
    }

    /** Rainbow: плавно переливающийся по кругу радужный оттенок. */
    private static void rainbow(DrawContext ctx, int sw, int sh, long now, float pw) {
        float h = (now % 12000L) / 12000f;
        ctx.fillGradient(0, 0, sw, sh, col(0.13f * pw, hsv(h)), col(0.13f * pw, hsv(h + 0.33f)));
        vignette(ctx, sw, sh, 0.22f * pw, hsv(h + 0.66f));
    }

    /** Pulse: «сердцебиение» цвета меню: виньетка бьётся в ритме и от краёв к центру бежит рамка-волна. */
    private static void pulse(DrawContext ctx, int sw, int sh, long now, float pw) {
        int acc = Theme.accent() & 0xFFFFFF;
        float b = (float) Math.pow(0.5 + 0.5 * Math.sin(now / 1000.0 * Math.PI * 2 * 0.9), 3);
        vignette(ctx, sw, sh, (0.25f + 0.55f * b) * pw, acc);
        ctx.fillGradient(0, 0, sw, sh, col(0.03f * b * pw, acc), col(0.07f * b * pw, acc));
        float ph = (now % 1111L) / 1111f;                                 // период ~0.9 Гц
        int in = (int) ((1f - ph) * sw * 0.10f);
        int th = Math.max(2, sh / 150);
        int c = col((1f - ph) * 0.5f * pw, acc);
        ctx.fill(in, in, sw - in, in + th, c);
        ctx.fill(in, sh - in - th, sw - in, sh - in, c);
        ctx.fill(in, in, in + th, sh - in, c);
        ctx.fill(sw - in - th, in, sw - in, sh - in, c);
    }

    /** Inferno: красно-оранжевая дымка и языки пламени, лижущие нижний край экрана. */
    private static void inferno(DrawContext ctx, int sw, int sh, long now, float pw) {
        float a = pw;
        ctx.fillGradient(0, 0, sw, sh, col(0.10f * a, 0x3A0500), col(0.06f * a, 0x7A1000));
        int n = 32, step = sw / n, wd = step + 1;
        for (int i = 0; i < n; i++) {
            double ph = now / 420.0 + i * 0.9;
            float hgt = sh * (0.10f + 0.07f * (float) Math.sin(ph) + 0.04f * (float) Math.sin(ph * 2.7 + i));
            float fl = 0.5f + 0.5f * (float) Math.sin(now / 130.0 + i * 1.3);
            int rgb = lerp(0xFF3000, 0xFFB020, fl);
            ctx.fillGradient(i * step, sh - (int) hgt, i * step + wd, sh, col(0f, rgb), col(0.30f * a, rgb));
        }
        vignette(ctx, sw, sh, 0.35f * a, 0x300000);
    }

    private static int hsv(float h) {
        h = frac(h) * 6f;
        int i = (int) h;
        float f = h - i, s = 0.75f;
        float p = 1f - s, q = 1f - s * f, t = 1f - s * (1f - f);
        float r, g, b;
        switch (i) {
            case 0 -> { r = 1; g = t; b = p; }
            case 1 -> { r = q; g = 1; b = p; }
            case 2 -> { r = p; g = 1; b = t; }
            case 3 -> { r = p; g = q; b = 1; }
            case 4 -> { r = t; g = p; b = 1; }
            default -> { r = 1; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /**
     * Soft Shader (облегчённая «цветокоррекция»): виньетка + цветовой тон. Один-два прямоугольника на кадр.
     * Soft = мягкое тепло, Vivid = насыщенный контраст, Cinematic = teal/orange.
     */
    private static void softShader(DrawContext ctx, int sw, int sh, int mode) {
        switch (mode) {
            case 1 -> {                       // Soft
                ctx.fill(0, 0, sw, sh, col(0.05f, 0xFFD9A0));
                vignette(ctx, sw, sh, 0.22f, 0x000000);
            }
            case 2 -> {                       // Vivid
                ctx.fillGradient(0, 0, sw, sh, col(0.07f, 0x5CB8FF), col(0.07f, 0xFF9A3C));
                vignette(ctx, sw, sh, 0.32f, 0x000000);
            }
            case 3 -> {                       // Cinematic
                ctx.fillGradient(0, 0, sw, sh, col(0.10f, 0x1E7A8C), col(0.12f, 0xFF8A3A));
                vignette(ctx, sw, sh, 0.45f, 0x000010);
                int bar = sh / 22;            // тонкие кино-полосы
                ctx.fill(0, 0, sw, bar, 0xFF000000);
                ctx.fill(0, sh - bar, sw, sh, 0xFF000000);
            }
            case 4 -> {                       // Neon: холодный верх / розовый низ + глубокая виньетка
                ctx.fillGradient(0, 0, sw, sh, col(0.08f, 0x00E5FF), col(0.09f, 0xFF2BD6));
                vignette(ctx, sw, sh, 0.30f, 0x10002A);
            }
            case 5 -> {                       // Moody: приглушённо-синий и тёмные края
                ctx.fill(0, 0, sw, sh, col(0.10f, 0x1A2030));
                vignette(ctx, sw, sh, 0.50f, 0x000000);
            }
            case 6 -> {                       // Bloom: мягкое свечение сверху
                ctx.fill(0, 0, sw, sh, col(0.05f, 0xFFFFFF));
                ctx.fillGradient(0, 0, sw, sh * 2 / 3, col(0.12f, 0xFFF2D0), col(0f, 0xFFF2D0));
                vignette(ctx, sw, sh, 0.12f, 0x000000);
            }
            default -> { }
        }
    }

    // ===================== мелкие помощники =====================
    /** виньетка: края темнее (8 ступенек на сторону, без текстур) */
    private static void vignette(DrawContext ctx, int sw, int sh, float strength, int rgb) {
        if (strength <= 0.005f) return;
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float t = 1f - (float) i / steps;
            int c = col(strength * t * t * 0.28f, rgb);
            int dx = (int) (sw * 0.16f * i / steps), dy = (int) (sh * 0.16f * i / steps);
            int bw = Math.max(1, sw / 60), bh = Math.max(1, sh / 60);
            ctx.fill(dx, 0, dx + bw, sh, c);
            ctx.fill(sw - dx - bw, 0, sw - dx, sh, c);
            ctx.fill(0, dy, sw, dy + bh, c);
            ctx.fill(0, sh - dy - bh, sw, sh - dy, c);
        }
    }

    /** закрашенный круг горизонтальными строками */
    private static void disc(DrawContext ctx, float cx, float cy, float r, int argb) {
        int ri = Math.max(1, Math.round(r));
        for (int dy = -ri; dy <= ri; dy++) {
            int half = (int) Math.sqrt((double) ri * ri - (double) dy * dy);
            ctx.fill((int) cx - half, (int) cy + dy, (int) cx + half, (int) cy + dy + 1, argb);
        }
    }

    private static int col(float alpha, int rgb) {
        int a = (int) (clamp(alpha) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float frac(float v) { return v - (float) Math.floor(v); }
    private static int lerp(int a, int b, float u) { return FxColor.lerp(a, b, u); }

    private WorldFx() {}
}
