package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Light Shader: «шейдер» без шейдеров. Цветокоррекция и виньетка несколькими полупрозрачными прямоугольниками
 * (1-2 градиента + рамка), без пост-процесса, FBO и лишних проходов рендера: FPS не просаживается, работает на слабых ПК.
 * Все стили статичные (без мерцания), поэтому безопасны для Comfort Mode.
 */
public final class LightShader {
    public static void render(DrawContext ctx, MinecraftClient mc) {
        int mode = Modules.LIGHT_SHADER.index;
        if (mode <= 0 || mc.world == null) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        switch (mode) {
            case 1 -> {                                                   // Soft: тёплая дымка
                ctx.fill(0, 0, sw, sh, argb(0.05f, 0xFFD9A0));
                vignette(ctx, sw, sh, 0.22f, 0x000000);
            }
            case 2 -> {                                                   // Vivid: контраст голубой/оранжевый
                ctx.fillGradient(0, 0, sw, sh, argb(0.07f, 0x5CB8FF), argb(0.07f, 0xFF9A3C));
                vignette(ctx, sw, sh, 0.32f, 0x000000);
            }
            case 3 -> {                                                   // Cinematic: teal/orange + полосы
                ctx.fillGradient(0, 0, sw, sh, argb(0.10f, 0x1E7A8C), argb(0.12f, 0xFF8A3A));
                vignette(ctx, sw, sh, 0.45f, 0x000010);
                int bar = sh / 22;
                ctx.fill(0, 0, sw, bar, 0xFF000000);
                ctx.fill(0, sh - bar, sw, sh, 0xFF000000);
            }
            case 4 -> {                                                   // Neon: циан сверху, маджента снизу
                ctx.fillGradient(0, 0, sw, sh, argb(0.08f, 0x00E5FF), argb(0.09f, 0xFF2BD6));
                vignette(ctx, sw, sh, 0.30f, 0x10002A);
            }
            case 5 -> {                                                   // Moody: приглушённо, тёмные края
                ctx.fill(0, 0, sw, sh, argb(0.10f, 0x1A2030));
                vignette(ctx, sw, sh, 0.50f, 0x000000);
            }
            case 6 -> {                                                   // Golden: золотой час
                ctx.fillGradient(0, 0, sw, sh, argb(0.12f, 0xFFB45A), argb(0.05f, 0xFF7A2A));
                vignette(ctx, sw, sh, 0.25f, 0x3A1400);
            }
            case 7 -> {                                                   // Frost: холодный светлый
                ctx.fillGradient(0, 0, sw, sh, argb(0.09f, 0xBFE4FF), argb(0.06f, 0x8FB8FF));
                vignette(ctx, sw, sh, 0.28f, 0x001028);
            }
            case 8 -> {                                                   // Blood: кроваво-красный мрак
                ctx.fillGradient(0, 0, sw, sh, argb(0.10f, 0xA00018), argb(0.14f, 0x400008));
                vignette(ctx, sw, sh, 0.55f, 0x200000);
            }
            default -> { }
        }
    }

    /** виньетка: 5 ступеней на сторону (при низком FPS 3), без текстур */
    private static void vignette(DrawContext ctx, int sw, int sh, float strength, int rgb) {
        int steps = Perf.reduced() ? 3 : 5;
        for (int i = 0; i < steps; i++) {
            float t = 1f - (float) i / steps;
            int c = argb(strength * t * t * 0.30f, rgb);
            int dx = (int) (sw * 0.15f * i / steps), dy = (int) (sh * 0.15f * i / steps);
            int bw = Math.max(1, sw / 40), bh = Math.max(1, sh / 40);
            ctx.fill(dx, 0, dx + bw, sh, c);
            ctx.fill(sw - dx - bw, 0, sw - dx, sh, c);
            ctx.fill(0, dy, sw, dy + bh, c);
            ctx.fill(0, sh - dy - bh, sw, sh - dy, c);
        }
    }

    private static int argb(float a, int rgb) {
        return ((int) (Math.max(0f, Math.min(1f, a)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    private LightShader() {}
}
