package com.neoncomfort;

public final class Theme {
    public static final int BG        = 0xCC0B0B14;
    public static final int PANEL     = 0xE6141425;
    public static final int ACCENT    = 0xFF7C4DFF;
    public static final int ACCENT_2  = 0xFF5B3BE0;
    public static final int OFF       = 0xFF2A2A3D;
    public static final int TEXT      = 0xFFFFFFFF;
    public static final int TEXT_DIM  = 0xFFA9A9C4;

    public static float pulse(double speedMs) {
        return (float) (Math.sin(System.currentTimeMillis() / speedMs) * 0.5 + 0.5);
    }

    public static int pulsingAccent() {
        int a = 110 + (int) (pulse(350) * 145);
        return (a << 24) | 0x7C4DFF;
    }

    private Theme() {}
}
