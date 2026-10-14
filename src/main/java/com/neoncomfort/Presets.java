package com.neoncomfort;

/** Готовые пресеты: одним нажатием выставляют сразу много пунктов (страницы 2-5), страница 1 и FPS Boost не меняются. */
public final class Presets {
    private static void set(Modules.Module m, String value) {
        if (m.values == null) return;
        for (int i = 0; i < m.values.length; i++) {
            if (m.values[i].equals(value)) {
                m.index = i;
                return;
            }
        }
    }

    /** idx: 0 = Custom (ничего не меняет), 1 = Destra, 2 = Neon, 3 = Minimal, 4 = PvP */
    public static void apply(int idx) {
        if (idx <= 0) return;
        Lite.clearEffects();
        if (idx == 1) {                       // Destra: зелёный неон, как в обзоре
            set(Modules.MENU_COLOR, "Lime");
            set(Modules.TWILIGHT, "Medium");
            set(Modules.TWILIGHT_STYLE, "Toxic");
            set(Modules.HAT, "China");
            set(Modules.TAIL, "Long");
            set(Modules.SLASH, "Wide");
            set(Modules.AURA_RINGS, "Ring");
            set(Modules.AURA_SPARKS, "Few");
            set(Modules.HIT_RING, "Ring");
            set(Modules.KILL_LIGHTNING, "Soft");
            Modules.BINDS.enabled = true;
            Modules.COMPASS.enabled = true;
        } else if (idx == 2) {                // Neon: фиолетовый стиль с крыльями и кубами
            set(Modules.MENU_COLOR, "Purple");
            set(Modules.TWILIGHT, "Medium");
            set(Modules.TWILIGHT_STYLE, "Violet");
            set(Modules.WINGS, "Void");
            set(Modules.HAT, "Halo");
            set(Modules.CUBES, "Normal");
            set(Modules.CUBE_LOOK, "Glow");
            set(Modules.AURA_RINGS, "Rings");
            set(Modules.AURA_SPARKS, "Few");
            set(Modules.ORBIT, "Normal");
            set(Modules.TAIL, "Short");
            set(Modules.SLASH, "Soft");
            set(Modules.KILL_EFFECT, "Soul");
        } else if (idx == 4) {                // PvP: ничего лишнего, только отметка попадания и мягкий взмах
            set(Modules.HIT_MARKER, "White");
            set(Modules.SLASH, "Soft");
        }
        // idx == 3 (Minimal): все эффекты выключены
        Config.save();
    }

    private Presets() {}
}
