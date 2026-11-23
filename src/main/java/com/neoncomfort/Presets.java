package com.neoncomfort;

import java.util.List;

/**
 * Готовые пресеты. Выбор пресета (Preset) НИЧЕГО не меняет: настройки меняются только после нажатия Apply Preset.
 * Перед применением все настройки запоминаются, и Undo Preset возвращает их (копия хранится в конфиге).
 * Пресет меняет страницы 2-5, а FPS Boost и остальное на странице Main не трогает (кроме цвета меню у Destra/Neon).
 */
public final class Presets {
    /** значения всех модулей до последнего применения пресета (в порядке Modules.ALL), null если нет */
    private static int[] backup;

    private static void set(Modules.Module m, String value) {
        if (m.values == null) return;
        for (int i = 0; i < m.values.length; i++) {
            if (m.values[i].equals(value)) {
                m.index = i;
                return;
            }
        }
    }

    /** idx: 0 = Custom (ничего не меняет), 1 = Destra, 2 = Neon, 3 = Minimal, 4 = PvP, 5 = Mine */
    public static void apply(int idx) {
        if (idx <= 0) return;
        backup = snapshot();                  // чтобы можно было отменить
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
            set(Modules.HOTBAR_GLOW, "Thin");
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
            set(Modules.NEON_STARS, "Normal");
            set(Modules.HOTBAR_GLOW, "Glow");
            set(Modules.FOOT_GLOW, "Soft");
        } else if (idx == 4) {                // PvP: ничего лишнего, только отметка попадания и мягкий взмах
            set(Modules.HIT_MARKER, "White");
            set(Modules.SLASH, "Soft");
        } else if (idx == 5) {                // Mine: ваши настройки, восстановленные по скриншотам
            set(Modules.MENU_COLOR, "Red");
            set(Modules.MENU_THEME, "Wine");
            set(Modules.TWILIGHT, "Light");
            set(Modules.TWILIGHT_STYLE, "Violet");
            set(Modules.SLASH, "Soft");
            set(Modules.SLASH_COLOR, "Cyan");
            set(Modules.HOTBAR_GLOW, "Glow");
            set(Modules.HOTBAR_COLOR, "Purple");
            set(Modules.HOTBAR_COLOR2, "Green");
            set(Modules.HIT_EFFECT, "Totem");
            set(Modules.HIT_AMOUNT, "More");
            set(Modules.HIT_GLOW, "Soft");
            set(Modules.HIT_EFFECT_COLOR, "Purple");
            set(Modules.TRAIL_EFFECT, "D-Ring");
            set(Modules.TRAIL_COLOR2, "White");
            set(Modules.TRAIL_AMOUNT, "Medium");
            set(Modules.TRAIL_EFFECT_COLOR, "Lime");
            set(Modules.PROJ_TRAIL, "Bright");
            set(Modules.WINGS, "Void");
            set(Modules.HAT, "China");
            set(Modules.TIME, "Night");
            set(Modules.CAPE, "Card");
            Modules.POTIONS.enabled = true;
            Modules.COMPASS.enabled = true;
        }
        if (idx == 6) {                       // Blood Moon: красное небо в мире, ночь и красная дымка
            set(Modules.SKY_MODE, "Blood Moon");
            set(Modules.SKY_POWER, "Normal");
            set(Modules.TIME, "Midnight");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Red");
        }
        // idx == 3 (Minimal): все эффекты выключены
        Config.save();
    }

    /** Visual Look: ставит только визуалы мира (небо, звёзды, радуга, воздух...), остальное не трогает. Отменяется Undo Preset. */
    public static void applyLook(int idx) {
        if (idx <= 0) return;
        backup = snapshot();
        set(Modules.SKY_STARS, "OFF");
        set(Modules.SHOOTING_STARS, "OFF");
        set(Modules.SKY_BODY, "OFF");
        set(Modules.RAINBOW, "OFF");
        set(Modules.SUN_GLOW, "OFF");
        set(Modules.AIR_FX, "OFF");
        set(Modules.BLOCK_GLOW, "OFF");
        set(Modules.RAIN_FX, "OFF");
        set(Modules.SKY_TINT, "OFF");
        set(Modules.LIGHT_SHADER, "OFF");
        if (idx == 1) {                       // Cinematic Night: звёздная ночь, большая луна, светлячки
            set(Modules.TIME, "Night");
            set(Modules.SKY_STARS, "Many");
            set(Modules.SHOOTING_STARS, "Rare");
            set(Modules.SKY_BODY, "Giant Moon");
            set(Modules.AIR_FX, "Fireflies");
            set(Modules.BLOCK_GLOW, "Outline");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Indigo");
        } else if (idx == 2) {                // Fantasy: планета с кольцом, радуга, лепестки, сияние
            set(Modules.TIME, "Night");
            set(Modules.SKY_STARS, "Normal");
            set(Modules.SHOOTING_STARS, "Often");
            set(Modules.SKY_BODY, "Ringed Planet");
            set(Modules.RAINBOW, "Vivid");
            set(Modules.AIR_FX, "Petals");
            set(Modules.BLOCK_GLOW, "Both");
            set(Modules.SKY_MODE, "Aurora");
            set(Modules.SKY_POWER, "Normal");
        } else if (idx == 3) {                // Golden Hour: тёплый закат, лучи солнца, радуга
            set(Modules.TIME, "Sunset");
            set(Modules.SUN_GLOW, "Strong");
            set(Modules.RAINBOW, "Soft");
            set(Modules.AIR_FX, "Spores");
            set(Modules.BLOCK_GLOW, "Outline");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Gold");
        } else if (idx == 4) {                // Winter Magic: снег, луна и сияние
            set(Modules.TIME, "Midnight");
            set(Modules.SKY_STARS, "Normal");
            set(Modules.SKY_BODY, "Giant Moon");
            set(Modules.AIR_FX, "Snow");
            set(Modules.SKY_MODE, "Aurora");
            set(Modules.SKY_POWER, "Normal");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Sky");
        } else if (idx == 5) {                // Blood Night: красная луна и искры
            set(Modules.TIME, "Midnight");
            set(Modules.SKY_STARS, "Few");
            set(Modules.SKY_BODY, "Blood Moon");
            set(Modules.AIR_FX, "Embers");
            set(Modules.SKY_MODE, "Blood Moon");
            set(Modules.SKY_POWER, "Normal");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Red");
        } else if (idx == 6) {                // Blood Storm: кровавая луна, кровавый дождь, красный мрак
            set(Modules.TIME, "Midnight");
            set(Modules.SKY_STARS, "Few");
            set(Modules.SKY_BODY, "Blood Moon");
            set(Modules.SKY_MODE, "Blood Moon");
            set(Modules.SKY_POWER, "Normal");
            set(Modules.RAIN_FX, "Blood Rain");
            set(Modules.SKY_TINT, "Blood");
            set(Modules.SKY_TINT_POWER, "Medium");
            set(Modules.RAIN_WHEN, "Always");
            set(Modules.LIGHT_SHADER, "Blood");
            set(Modules.AIR_FX, "Embers");
            set(Modules.FOG, "Light");
            set(Modules.FOG_COLOR, "Red");
        }
        Config.save();
    }

    /** Вернуть настройки, которые были до последнего применения пресета. */
    public static void undo() {
        List<Modules.Module> all = Modules.ALL;
        if (backup == null || backup.length != all.size()) return;
        for (int i = 0; i < backup.length; i++) {
            Modules.Module m = all.get(i);
            if (m == Modules.PRESET_APPLY || m == Modules.PRESET_UNDO) continue;
            if (m.isOption()) {
                if (backup[i] >= 0 && backup[i] < m.values.length) m.index = backup[i];
            } else {
                m.enabled = backup[i] != 0;
            }
        }
        backup = null;
        Config.save();
    }

    private static int[] snapshot() {
        List<Modules.Module> all = Modules.ALL;
        int[] a = new int[all.size()];
        for (int i = 0; i < a.length; i++) {
            Modules.Module m = all.get(i);
            a[i] = m.isOption() ? m.index : (m.enabled ? 1 : 0);
        }
        return a;
    }

    // --- сохранение копии настроек в конфиг ---
    static String serialize() {
        if (backup == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backup.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(backup[i]);
        }
        return sb.toString();
    }

    static void deserialize(String s) {
        backup = null;
        if (s == null || s.trim().isEmpty()) return;
        try {
            String[] t = s.trim().split(",");
            int[] a = new int[t.length];
            for (int i = 0; i < t.length; i++) a[i] = Integer.parseInt(t[i].trim());
            backup = a;
        } catch (NumberFormatException e) {
            backup = null;
        }
    }

    private Presets() {}
}
