package com.neoncomfort;

public final class Theme {
    public static final int BG        = 0xCC0B0B14;
    public static final int PANEL     = 0xE6141425;
    public static final int OFF       = 0xFF2A2A3D;
    public static final int TEXT      = 0xFFFFFFFF;
    public static final int TEXT_DIM  = 0xFFA9A9C4;

    public static final String[] COLOR_NAMES = {
            "Purple", "Blue", "Green", "Red", "Pink", "Cyan", "Orange", "Yellow", "White",
            "Lime", "Teal", "Gold", "Sky", "Magenta", "Mint", "Coral", "Indigo" };
    private static final int[] COLORS = {
            0x7C4DFF, 0x2979FF, 0x00C853, 0xFF5252, 0xFF4081, 0x00B8D4, 0xFF9100, 0xFFEA00, 0xFFFFFF,
            0xAEEA00, 0x009688, 0xFFC107, 0x40C4FF, 0xD500F9, 0x69F0AE, 0xFF6E40, 0x536DFE };

    /** фон панели меню: Dark, Midnight, Wine, Forest, Graphite */
    private static final int[] PANELS = { 0xE6141425, 0xE60B1530, 0xE62A0F1A, 0xE60F2418, 0xE61C1C1C };

    public static int panel() { return PANELS[Modules.MENU_THEME.index % PANELS.length]; }

    public static int colorCount() { return COLORS.length; }

    public static int colorRgb(int i) { return COLORS[i % COLORS.length]; }

    /** плавно меняющийся цвет радуги (RGB без альфы) */
    public static int rainbowRgb() { return rainbowRgb(0L); }

    /** радуга со сдвигом по времени (для разных цветов в хвосте) */
    public static int rainbowRgb(long shiftMs) {
        float h6 = ((System.currentTimeMillis() + shiftMs) % 3000L + 3000L) % 3000L / 3000f * 6f;
        int i = (int) h6;
        float f = h6 - i;
        int t = (int) (255 * f), q = 255 - t;
        int r, g, b;
        switch (i % 6) {
            case 0 -> { r = 255; g = t; b = 0; }
            case 1 -> { r = q; g = 255; b = 0; }
            case 2 -> { r = 0; g = 255; b = t; }
            case 3 -> { r = 0; g = q; b = 255; }
            case 4 -> { r = t; g = 0; b = 255; }
            default -> { r = 255; g = 0; b = q; }
        }
        return (r << 16) | (g << 8) | b;
    }

    private static int rgb() {
        return COLORS[Modules.MENU_COLOR.index % COLORS.length];
    }

    /** main accent colour (opaque) */
    public static int accent() {
        return 0xFF000000 | rgb();
    }

    /** colour of the custom crosshair (0 = same as menu) */
    public static int crosshairColor() {
        int i = Modules.CROSSHAIR_COLOR.index;
        if (i <= 0) return accent();
        if (i > COLORS.length) return 0xFF000000 | rainbowRgb();
        return 0xFF000000 | COLORS[i - 1];
    }

    /** darker accent for enabled buttons */
    public static int accent2() {
        int r = (int) (((rgb() >> 16) & 0xFF) * 0.75);
        int g = (int) (((rgb() >> 8) & 0xFF) * 0.75);
        int b = (int) ((rgb() & 0xFF) * 0.75);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 0..1 smooth pulse */
    public static float pulse(double speedMs) {
        return (float) (Math.sin(System.currentTimeMillis() / speedMs) * 0.5 + 0.5);
    }

    /** accent colour with pulsing alpha */
    public static int pulsingAccent() {
        int a = 110 + (int) (pulse(350) * 145);
        return (a << 24) | rgb();
    }

    private Theme() {}
}
