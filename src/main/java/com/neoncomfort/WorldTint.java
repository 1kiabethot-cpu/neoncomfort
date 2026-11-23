package com.neoncomfort;

import net.minecraft.util.math.Vec3d;

/**
 * Настоящая окраска мира через миксины: цвет неба / облаков / тумана (Sky Tint) и цвет самого ванильного дождя (Rain Real Color).
 * Без наложений на экран и без лишних проходов: меняется только цвет, который игра и так считает каждый кадр.
 */
public final class WorldTint {
    /** Blood, Sunset, Violet, Toxic, Ice, Gold, Ocean */
    private static final int[] SKY = { 0xC01010, 0xFF7A3A, 0x8A3CFF, 0x44FF33, 0x9FD8FF, 0xFFC040, 0x1E8CFF };

    /** флаг: миксин дождя уже поставил цвет, значит его надо вернуть */
    public static boolean rainTinted = false;

    private static int skyTint() {
        int m = Modules.SKY_TINT.index;
        if (m <= 0) return -1;
        if (m <= SKY.length) return SKY[m - 1];
        switch (Seasons.current()) {                       // режим Season: цвет по текущей поре года
            case 1: return 0xFFC8E6;
            case 2: return 0xFFD27A;
            case 3: return 0xFF8A2A;
            case 4: return 0xBFE4FF;
            default: return -1;
        }
    }

    private static float power() {
        int p = Modules.SKY_TINT_POWER.index;
        return p == 1 ? 0.30f : (p == 2 ? 0.75f : 0.50f);
    }

    /** смешивание с сохранением яркости: ночью небо остаётся тёмным, но слегка светится цветом */
    private static float[] mix(float r, float g, float b, int tint, float k) {
        float tr = ((tint >> 16) & 255) / 255f, tg = ((tint >> 8) & 255) / 255f, tb = (tint & 255) / 255f;
        float scale = Math.max(Math.max(r, Math.max(g, b)), 0.04f);
        float glow = 0.05f * k;
        return new float[] {
            clamp(r * (1f - k) + tr * scale * k + tr * glow),
            clamp(g * (1f - k) + tg * scale * k + tg * glow),
            clamp(b * (1f - k) + tb * scale * k + tb * glow)
        };
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** Результат getSkyColor / getCloudsColor: в 1.21.4 это int (ARGB), в старых версиях Vec3d: поддерживаются оба */
    public static Object tint(Object v, float strength) {
        int t = skyTint();
        if (t < 0 || v == null) return v;
        float k = power() * strength;
        if (v instanceof Integer) {
            int i = (Integer) v;
            float[] o = mix(((i >> 16) & 255) / 255f, ((i >> 8) & 255) / 255f, (i & 255) / 255f, t, k);
            return (i & 0xFF000000) | ((int) (o[0] * 255f) << 16) | ((int) (o[1] * 255f) << 8) | (int) (o[2] * 255f);
        }
        if (v instanceof Vec3d) {
            Vec3d d = (Vec3d) v;
            float[] o = mix((float) d.x, (float) d.y, (float) d.z, t, k);
            return new Vec3d(o[0], o[1], o[2]);
        }
        return v;
    }

    /** цвет ванильного дождя или null, если дождь не перекрашиваем */
    public static float[] rain() {
        int c = Modules.RAIN_REAL.index;
        int rgb = -1;
        if (c > 0 && c <= Theme.colorCount()) rgb = Theme.colorRgb(c - 1) & 0xFFFFFF;
        else if (Modules.RAIN_FX.index > 0) {
            switch (Modules.RAIN_FX.index) {
                case 1: rgb = 0xD01824; break;                                   // Blood
                case 2: rgb = 0xFF8A20; break;                                   // Ember
                case 3: rgb = Theme.accent() & 0xFFFFFF; break;                  // Neon
                case 4: rgb = 0x5CFF3A; break;                                   // Toxic
                default: rgb = 0xA0A0A0; break;                                  // Ash
            }
        }
        if (rgb < 0) return null;
        return new float[] { ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f };
    }

    private WorldTint() {}
}
