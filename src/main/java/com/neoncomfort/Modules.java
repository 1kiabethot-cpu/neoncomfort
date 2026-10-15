package com.neoncomfort;

import java.util.List;

public final class Modules {
    public static final class Module {
        public final String name;
        public boolean enabled;
        public final String[] values;      // null для обычных переключателей
        public final boolean zeroIsOff;    // значение №0 = выключено
        public int index;

        private Module(String name, boolean enabled, boolean zeroIsOff, String[] values) {
            this.name = name;
            this.enabled = enabled;
            this.zeroIsOff = zeroIsOff;
            this.values = values;
        }

        Module(String name, boolean enabled) {
            this(name, enabled, false, null);
        }

        static Module option(String name, boolean zeroIsOff, String... values) {
            return new Module(name, false, zeroIsOff, values);
        }

        public boolean isOption() { return values != null; }

        public boolean isOn() {
            return isOption() ? (!zeroIsOff || index > 0) : enabled;
        }

        public void click() {
            if (isOption()) index = (index + 1) % values.length;
            else enabled = !enabled;
        }

        public void toggle() { click(); }

        public String label() {
            return isOption() ? values[index] : (enabled ? "ON" : "OFF");
        }
    }

    public static final Module WATERMARK = new Module("Watermark", true);
    public static final Module COORDS    = new Module("Coordinates", true);
    public static final Module ARMOR     = new Module("Armor HUD", true);
    public static final Module PING_TIME = new Module("Ping & Time", true);
    public static final Module TARGET    = new Module("Target HP", true);
    public static final Module FULLBRIGHT = new Module("Fullbright", false);
    public static final Module AUTOSPRINT = new Module("Auto Sprint", false);
    public static final Module ZOOM      = new Module("Zoom (C)", true);
    public static final Module NEARBY_ARMOR = new Module("Nearby Armor", true);

    // --- визуал ---
    // 0 = Thin (ванильный прицел скрыт ресурсом мода), последний = None (без прицела)
    public static final Module CROSSHAIR  = Module.option("Crosshair", false,
            "Thin", "Dot", "Cross", "Plus", "Square",
            "Circle", "Diamond", "X", "Corners", "Plus Dot", "Ring Dot",
            "T-Shape", "Big Ring", "Brackets", "Star", "None");
    public static final Module CROSSHAIR_COLOR = Module.option("Crosshair Color", false, menuPlus());
    public static final Module TRAIL = Module.option("Trail", true, trailValues());
    public static final Module MENU_COLOR = Module.option("Menu Color", false, Theme.COLOR_NAMES);
    public static final Module NO_HURT_CAM = new Module("No Hurt Cam", false);
    public static final Module LOW_FIRE   = new Module("Low Fire", false);
    public static final Module HIT_SOUND  = Module.option("Hit Sound", true,
            "OFF", "Orb", "Bell", "Pling", "Ding", "Levelup", "Chime", "Crit");
    public static final Module WEATHER    = Module.option("Weather", true,
            "Default", "Clear", "Rain", "Thunder");

    // --- страница 2 ---
    public static final Module TWILIGHT = Module.option("Twilight", true,
            "OFF", "Light", "Medium", "Strong");
    public static final Module CROSSHAIR_SIZE = Module.option("Crosshair Size", false,
            "Normal", "Small", "Big", "Huge");
    public static final Module CROSSHAIR_OUTLINE = new Module("Crosshair Outline", false);
    public static final Module HIT_COLOR = Module.option("Hit Color", true, trailValues());
    public static final Module HIT_VOLUME = Module.option("Hit Volume", false,
            "Normal", "Quiet", "Loud");

    public static final Module TRAIL_AMOUNT = Module.option("Trail Amount", false,
            "Medium", "Less", "More");
    public static final Module HIT_AMOUNT = Module.option("Hit Amount", false,
            "Medium", "Less", "More");
    public static final Module TWILIGHT_STYLE = Module.option("Twilight Style", false,
            "Sunset", "Pink", "Aurora", "Violet", "Gold", "Toxic");
    public static final Module HIT_EFFECT = Module.option("Hit Effect", false, effectNames());
    public static final Module TRAIL_EFFECT = Module.option("Trail Effect", false, effectNames());
    public static final Module HIT_COLOR2 = Module.option("Hit Color 2", true, color2Values());
    public static final Module TRAIL_COLOR2 = Module.option("Trail Color 2", true, color2Values());
    public static final Module HIT_GLOW = Module.option("Hit Glow", true, "OFF", "Soft", "Bright", "Spark");
    public static final Module TRAIL_GLOW = Module.option("Trail Glow", true, "OFF", "Soft", "Bright", "Spark");
    public static final Module HIT_EFFECT_COLOR = Module.option("Hit FX Color", false, origValues());
    public static final Module TRAIL_EFFECT_COLOR = Module.option("Trail FX Color", false, origValues());
    public static final Module MENU_THEME = Module.option("Menu Theme", false,
            "Dark", "Midnight", "Wine", "Forest", "Graphite");
    public static final Module HUD_SIZE = Module.option("HUD Size", false,
            "80%", "70%", "90%", "100%", "120%");
    public static final Module HIT_MARKER = Module.option("Hit Marker", true, color2Values());
    public static final Module KILL_EFFECT = Module.option("Kill Effect", true,
            "OFF", "Totem", "Firework", "Soul", "Crit", "Heart", "Boom");
    public static final Module KILL_COLOR = Module.option("Kill Color", true, color2Values());
    public static final Module SCREEN_FX = Module.option("Screen FX", true,
            "OFF", "Snow", "Sparkle", "Firefly", "Rain", "Bubbles", "Embers", "Petals");
    public static final Module SCREEN_FX_AMOUNT = Module.option("Screen FX Amount", false,
            "Medium", "Less", "More");
    public static final Module SCREEN_FX_COLOR = Module.option("Screen FX Color", false, autoValues());
    public static final Module FPS_BOOST = Module.option("FPS Boost", true,
            "OFF", "Balanced", "Max");

    // --- косметика ---
    public static final Module WINGS = Module.option("Wings", true,
            "OFF", "Void", "Gold", "Ember", "Frost", "Rose");
    public static final Module HAT = Module.option("Hat", true,
            "OFF", "Crown", "Halo", "Top Hat", "Cat Ears", "Horns", "Wizard", "China");
    public static final Module CUBES = Module.option("Cubes", true,
            "OFF", "Few", "Normal", "Many");
    public static final Module CUBE_MOTION = Module.option("Cube Motion", false,
            "Circle", "Oval", "Chaos", "Spiral");
    public static final Module CUBE_LOOK = Module.option("Cube Look", false,
            "Solid", "Glass", "Glow");
    public static final Module AURA_RINGS = Module.option("Aura Rings", true,
            "OFF", "Ring", "Rings", "Circle");
    public static final Module HIT_RING = Module.option("Hit Ring", true,
            "OFF", "Ring", "Double", "Burst");
    public static final Module AURA_COLOR = Module.option("Aura Color", false, menuPlus());

    // --- настройка косметики (страница 5) ---
    public static final Module WING_SIZE = Module.option("Wing Size", false,
            "Normal", "Small", "Big");
    public static final Module WING_FLAP = Module.option("Wing Flap", false,
            "Normal", "Slow", "Fast", "Off");
    public static final Module CUBE_SIZE = Module.option("Cube Size", false,
            "Normal", "Small", "Big");
    public static final Module AURA_SIZE = Module.option("Aura Size", false,
            "Normal", "Small", "Big");
    public static final Module AURA_SPEED = Module.option("Aura Speed", false,
            "Normal", "Slow", "Fast");
    public static final Module AURA_GLOW = Module.option("Aura Glow", false,
            "Normal", "Soft", "Bright");
    public static final Module HIT_RING_SIZE = Module.option("Hit Ring Size", false,
            "Normal", "Small", "Big");
    public static final Module CUBE_BURST = Module.option("Cube Burst", true,
            "OFF", "Soft", "Strong");
    public static final Module AURA_SPARKS = Module.option("Aura Sparks", true,
            "OFF", "Few", "Many");

    // --- стиль «Destra»: список биндов, световой взмах меча ---
    public static final Module BINDS = new Module("Binds List", true);
    public static final Module SLASH = Module.option("Slash FX", true, "OFF", "Soft", "Wide", "Double");
    public static final Module SLASH_COLOR = Module.option("Slash Color", false, menuPlus());
    public static final Module TAIL = Module.option("Tail", true, "OFF", "Short", "Long");

    // --- версия 2.1: пресеты, орбитальные клинки, молния при убийстве, компас ---
    public static final Module PRESET = Module.option("Preset", false, "Custom", "Destra", "Neon", "Minimal", "PvP");
    public static final Module ORBIT = Module.option("Orbit Blades", true, "OFF", "Few", "Normal", "Many");
    public static final Module KILL_LIGHTNING = Module.option("Kill Lightning", true, "OFF", "Soft", "Strong");
    public static final Module LIGHTNING_COLOR = Module.option("Lightning Color", false, menuPlus());
    public static final Module COMPASS = new Module("Compass Bar", false);

    // --- версия 2.2: следы снарядов, круги прыжка, светящиеся шаги, свечение хотбара, неоновые звёзды ---
    // у каждого эффекта: основной цвет (Color) и второй цвет для смешивания (Mix, OFF = без смешивания)
    public static final Module PROJ_TRAIL = Module.option("Projectile Trail", true, "OFF", "Soft", "Bright", "Spark");
    public static final Module PROJ_COLOR = Module.option("Projectile Color", false, menuPlus());
    public static final Module PROJ_COLOR2 = Module.option("Projectile Mix", true, color2Values());
    public static final Module JUMP_CIRCLE = Module.option("Jump Circle", true, "OFF", "Jump", "Land", "Both");
    public static final Module JUMP_COLOR = Module.option("Jump Color", false, menuPlus());
    public static final Module JUMP_COLOR2 = Module.option("Jump Mix", true, color2Values());
    public static final Module FOOT_GLOW = Module.option("Footstep Glow", true, "OFF", "Soft", "Bright");
    public static final Module FOOT_COLOR = Module.option("Footstep Color", false, menuPlus());
    public static final Module FOOT_COLOR2 = Module.option("Footstep Mix", true, color2Values());
    public static final Module HOTBAR_GLOW = Module.option("Hotbar Glow", true, "OFF", "Thin", "Thick", "Glow");
    public static final Module HOTBAR_COLOR = Module.option("Hotbar Color", false, menuPlus());
    public static final Module HOTBAR_COLOR2 = Module.option("Hotbar Mix", true, color2Values());
    public static final Module NEON_STARS = Module.option("Neon Stars", true, "OFF", "Few", "Normal", "Many");
    public static final Module STAR_COLOR = Module.option("Stars Color", false, menuPlus());
    public static final Module STAR_COLOR2 = Module.option("Stars Mix", true, color2Values());

    public static final List<Module> PAGE1 = List.of(
            WATERMARK, COORDS, ARMOR, PING_TIME, TARGET, FULLBRIGHT, AUTOSPRINT, ZOOM, NEARBY_ARMOR,
            CROSSHAIR, CROSSHAIR_COLOR, TRAIL, MENU_COLOR, NO_HURT_CAM, LOW_FIRE, HIT_SOUND, WEATHER);
    public static final List<Module> PAGE2 = List.of(
            PRESET, TWILIGHT, TWILIGHT_STYLE, CROSSHAIR_SIZE, CROSSHAIR_OUTLINE, HIT_VOLUME, FPS_BOOST,
            MENU_THEME, HUD_SIZE, HIT_MARKER, BINDS, SLASH, SLASH_COLOR, TAIL, HOTBAR_GLOW, HOTBAR_COLOR, HOTBAR_COLOR2);
    public static final List<Module> PAGE3 = List.of(
            HIT_COLOR, HIT_COLOR2, HIT_EFFECT, HIT_AMOUNT, HIT_GLOW, HIT_EFFECT_COLOR,
            TRAIL_COLOR2, TRAIL_EFFECT, TRAIL_AMOUNT, TRAIL_GLOW, TRAIL_EFFECT_COLOR,
            PROJ_TRAIL, PROJ_COLOR, PROJ_COLOR2, FOOT_GLOW, FOOT_COLOR, FOOT_COLOR2);
    public static final List<Module> PAGE4 = List.of(
            SCREEN_FX, SCREEN_FX_AMOUNT, SCREEN_FX_COLOR, KILL_EFFECT, KILL_COLOR,
            WINGS, HAT, CUBES, CUBE_MOTION, CUBE_LOOK, AURA_RINGS, HIT_RING, AURA_COLOR,
            JUMP_CIRCLE, JUMP_COLOR, JUMP_COLOR2);
    public static final List<Module> PAGE5 = List.of(
            WING_SIZE, WING_FLAP, CUBE_SIZE, AURA_SIZE, AURA_SPEED, AURA_GLOW, HIT_RING_SIZE,
            CUBE_BURST, AURA_SPARKS, ORBIT, KILL_LIGHTNING, LIGHTNING_COLOR, COMPASS,
            NEON_STARS, STAR_COLOR, STAR_COLOR2);
    public static final List<List<Module>> PAGES = List.of(PAGE1, PAGE2, PAGE3, PAGE4, PAGE5);

    /** Упрощённое меню: только самое нужное на одной странице */
    public static final Module MENU_SIMPLE = new Module("Simple Menu", false);
    /** Язык меню: English / Українська */
    public static final Module LANG = Module.option("Language", false, "English", "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430");
    /** Лайт-режим: выключает всё, кроме FPS Boost и Fullbright */
    public static final Module LITE = new Module("Lite Mode", false);
    public static final List<Module> SIMPLE = List.of(
            CROSSHAIR, CROSSHAIR_COLOR, HIT_SOUND,
            HIT_COLOR, HIT_EFFECT, TRAIL,
            TRAIL_EFFECT, TWILIGHT, SCREEN_FX,
            FPS_BOOST, FULLBRIGHT, ZOOM);

    /** все модули (для сохранения конфига) */
    public static final List<Module> ALL;
    static {
        List<Module> all = new java.util.ArrayList<>();
        for (List<Module> pg : PAGES) all.addAll(pg);
        all.add(MENU_SIMPLE);
        all.add(LANG);
        all.add(LITE);
        ALL = java.util.Collections.unmodifiableList(all);
    }

    private static String[] effectNames() {
        return new String[] { "Dust", "Crit", "Magic", "Star", "Fire", "Soul", "Spark",
                "Heart", "Note", "Snow", "Glow", "Totem", "Rune", "Happy",
                "D-Heart", "D-Star", "D-Ring", "D-Spiral" };
    }

    private static String[] color2Values() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 1];
        a[0] = "OFF";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
        return a;
    }

    /** Цвет самого эффекта: Original = как в игре, дальше цвета и Rainbow */
    private static String[] origValues() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 2];
        a[0] = "Original";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
        a[n + 1] = "Rainbow";
        return a;
    }

    private static String[] autoValues() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 2];
        a[0] = "Auto";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
        a[n + 1] = "Rainbow";
        return a;
    }

    private static String[] menuPlus() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 2];
        a[0] = "Menu";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
        a[n + 1] = "Rainbow";
        return a;
    }

    private static String[] trailValues() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 2];
        a[0] = "OFF";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
        a[n + 1] = "Rainbow";
        return a;
    }

    private Modules() {}
    }
