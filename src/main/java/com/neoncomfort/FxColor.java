package com.neoncomfort;

/** Цвет эффекта: основной цвет (Menu / цвета / Rainbow) и необязательный второй цвет (Mix) для смешивания. */
public final class FxColor {
    static int base(Modules.Module c1, long shiftMs) {
        int i = c1.index;
        if (i <= 0) return Theme.accent() & 0xFFFFFF;
        if (i > Theme.colorCount()) return Theme.rainbowRgb(shiftMs) & 0xFFFFFF;
        return Theme.colorRgb(i - 1) & 0xFFFFFF;
    }

    static int lerp(int a, int b, float u) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * u);
        int g = (int) (ag + (bg - ag) * u);
        int bl = (int) (ab + (bb - ab) * u);
        return (r << 16) | (g << 8) | bl;
    }

    /** u от 0 до 1: доля второго цвета; если Mix выключен, всегда основной цвет */
    static int pick(Modules.Module c1, Modules.Module c2, float u, long shiftMs) {
        int a = base(c1, shiftMs);
        int i2 = c2.index;
        if (i2 <= 0) return a;
        int b = Theme.colorRgb(i2 - 1) & 0xFFFFFF;
        return lerp(a, b, Math.max(0f, Math.min(1f, u)));
    }

    private FxColor() {}
}
