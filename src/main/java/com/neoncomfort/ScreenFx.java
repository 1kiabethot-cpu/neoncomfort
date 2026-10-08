package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Частицы на экране (рисуются поверх игры, почти без нагрузки).
 * Стили: 1 Snow, 2 Sparkle, 3 Firefly, 4 Rain, 5 Bubbles, 6 Embers, 7 Petals.
 */
public final class ScreenFx {
    private static final class P {
        float x, y, vx, vy, size, age, life, phase;
    }

    private static final ArrayList<P> LIST = new ArrayList<>();
    private static long last = 0L;

    /** максимум частиц: Medium, Less, More */
    private static final int[] MAX = { 90, 40, 180 };
    /** авто-цвета по стилям (индекс 0 не используется) */
    private static final int[] AUTO = {
            0x000000, 0xFFFFFF, 0xFFF4B0, 0xC8FF5A, 0x8CB4FF, 0xA0E8FF, 0xFF8A2A, 0xFFB0D0 };

    public static void render(DrawContext ctx, MinecraftClient mc) {
        int style = Modules.SCREEN_FX.index;
        if (style <= 0 || style >= AUTO.length) {
            LIST.clear();
            last = 0L;
            return;
        }
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        long now = System.nanoTime();
        float dt = last == 0L ? 0.016f : Math.min(0.05f, (now - last) / 1_000_000_000f);
        last = now;

        int max = MAX[Modules.SCREEN_FX_AMOUNT.index % MAX.length];
        int spawn = Math.min(3, Math.max(0, max - LIST.size()));
        for (int i = 0; i < spawn; i++) {
            if (Math.random() < 0.6) LIST.add(make(style, w, h));
        }

        int rgb = baseColor(style);
        Iterator<P> it = LIST.iterator();
        while (it.hasNext()) {
            P p = it.next();
            p.age += dt;
            p.x += p.vx * dt + (float) Math.sin(p.age * 1.5f + p.phase) * sway(style) * dt;
            p.y += p.vy * dt;
            if (p.age > p.life || p.y < -24 || p.y > h + 24 || p.x < -24 || p.x > w + 24) {
                it.remove();
                continue;
            }
            draw(ctx, p, style, rgb);
        }
    }

    private static int baseColor(int style) {
        int m = Modules.SCREEN_FX_COLOR.index;
        if (m == 0) return AUTO[style];
        if (m > Theme.colorCount()) return Theme.rainbowRgb(0L);
        return Theme.colorRgb(m - 1);
    }

    private static float sway(int style) {
        switch (style) {
            case 1: return 12f;
            case 5: return 8f;
            case 6: return 6f;
            case 7: return 15f;
            default: return 0f;
        }
    }

    private static float rnd(float a, float b) {
        return a + (float) Math.random() * (b - a);
    }

    private static P make(int style, int w, int h) {
        P p = new P();
        p.phase = rnd(0f, 6.28f);
        p.x = rnd(0f, w);
        switch (style) {
            case 1:                                   // Snow
                p.y = -4; p.vx = rnd(-6f, 6f); p.vy = rnd(25f, 55f); p.size = rnd(1f, 3f); p.life = 14f;
                break;
            case 2:                                   // Sparkle
                p.y = rnd(0f, h); p.vx = rnd(-3f, 3f); p.vy = rnd(-3f, 3f); p.size = rnd(1f, 2f); p.life = rnd(1.5f, 3f);
                break;
            case 3:                                   // Firefly
                p.y = rnd(h * 0.3f, h); p.vx = rnd(-8f, 8f); p.vy = rnd(-8f, 8f); p.size = 2f; p.life = rnd(4f, 8f);
                break;
            case 4:                                   // Rain
                p.y = -10; p.vx = -18f; p.vy = rnd(280f, 380f); p.size = rnd(6f, 12f); p.life = 3f;
                break;
            case 5:                                   // Bubbles
                p.y = h + 4; p.vx = rnd(-4f, 4f); p.vy = -rnd(18f, 40f); p.size = rnd(3f, 6f); p.life = 12f;
                break;
            case 6:                                   // Embers
                p.y = h + 4; p.vx = rnd(-10f, 10f); p.vy = -rnd(25f, 60f); p.size = rnd(1f, 2f); p.life = 6f;
                break;
            default:                                  // Petals
                p.x = rnd(-10f, w * 0.7f);
                p.y = -6; p.vx = rnd(10f, 25f); p.vy = rnd(20f, 40f); p.size = rnd(2f, 3f); p.life = 14f;
                break;
        }
        return p;
    }

    private static void draw(DrawContext ctx, P p, int style, int rgb) {
        // плавное появление и исчезновение
        float f = Math.min(1f, Math.min(p.age / 0.5f, (p.life - p.age) / 1.0f));
        if (f <= 0f) return;
        if (style == 2) f *= Math.abs((float) Math.sin(p.age * 8f + p.phase));
        if (style == 3) f *= 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(p.age * 3f + p.phase));

        float base = style == 4 ? 0.55f : 0.9f;
        int a = (int) (f * base * 255f);
        if (a <= 0) return;
        int c = (a << 24) | rgb;
        int x = (int) p.x, y = (int) p.y;
        int s = Math.max(1, (int) p.size);

        switch (style) {
            case 1 -> ctx.fill(x, y, x + s, y + s, c);
            case 2 -> {
                ctx.fill(x - 1, y, x + 2, y + 1, c);
                ctx.fill(x, y - 1, x + 1, y + 2, c);
            }
            case 3 -> {
                ctx.fill(x - 2, y - 2, x + 4, y + 4, ((a / 5) << 24) | rgb);
                ctx.fill(x, y, x + 2, y + 2, c);
            }
            case 4 -> ctx.fill(x, y, x + 1, y + s, c);
            case 5 -> {
                ctx.fill(x, y, x + s, y + 1, c);
                ctx.fill(x, y + s - 1, x + s, y + s, c);
                ctx.fill(x, y, x + 1, y + s, c);
                ctx.fill(x + s - 1, y, x + s, y + s, c);
            }
            case 6 -> {
                ctx.fill(x - 1, y - 1, x + s + 1, y + s + 1, ((a / 4) << 24) | rgb);
                ctx.fill(x, y, x + s, y + s, c);
            }
            default -> {
                ctx.fill(x, y, x + s, y + s, c);
                ctx.fill(x + 1, y - 1, x + s + 1, y + s - 1, ((a / 2) << 24) | rgb);
            }
        }
    }

    private ScreenFx() {}
}
