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
            "Sunset", "Pink", "Aurora", "Violet", "Gold");
    public static final Module HIT_EFFECT = Module.option("Hit Effect", false, effectNames());
    public static final Module TRAIL_EFFECT = Module.option("Trail Effect", false, effectNames());
    public static final Module HIT_COLOR2 = Module.option("Hit Color 2", true, color2Values());
    public static final Module TRAIL_COLOR2 = Module.option("Trail Color 2", true, color2Values());
    public static final Module HIT_GLOW = Module.option("Hit Glow", true, "OFF", "Soft", "Bright", "Spark");
    public static final Module TRAIL_GLOW = Module.option("Trail Glow", true, "OFF", "Soft", "Bright", "Spark");
    public static final Module SCREEN_FX = Module.option("Screen FX", true,
            "OFF", "Snow", "Sparkle", "Firefly", "Rain", "Bubbles", "Embers", "Petals");
    public static final Module SCREEN_FX_AMOUNT = Module.option("Screen FX Amount", false,
            "Medium", "Less", "More");
    public static final Module SCREEN_FX_COLOR = Module.option("Screen FX Color", false, autoValues());
    public static final Module FPS_BOOST = Module.option("FPS Boost", true,
            "OFF", "Balanced", "Max");

    public static final List<Module> PAGE1 = List.of(
            WATERMARK, COORDS, ARMOR, PING_TIME, TARGET, FULLBRIGHT, AUTOSPRINT, ZOOM, NEARBY_ARMOR,
            CROSSHAIR, CROSSHAIR_COLOR, TRAIL, MENU_COLOR, NO_HURT_CAM, LOW_FIRE, HIT_SOUND, WEATHER);
    public static final List<Module> PAGE2 = List.of(
            TWILIGHT, TWILIGHT_STYLE, CROSSHAIR_SIZE, CROSSHAIR_OUTLINE, HIT_VOLUME, FPS_BOOST);
    public static final List<Module> PAGE3 = List.of(
            HIT_COLOR, HIT_COLOR2, HIT_EFFECT, HIT_AMOUNT, HIT_GLOW, TRAIL_COLOR2,
            TRAIL_EFFECT, TRAIL_AMOUNT, TRAIL_GLOW);
    public static final List<Module> PAGE4 = List.of(
            SCREEN_FX, SCREEN_FX_AMOUNT, SCREEN_FX_COLOR);
    public static final List<List<Module>> PAGES = List.of(PAGE1, PAGE2, PAGE3, PAGE4);

    /** все модули (для сохранения конфига) */
    public static final List<Module> ALL;
    static {
        List<Module> all = new java.util.ArrayList<>();
        for (List<Module> pg : PAGES) all.addAll(pg);
        ALL = java.util.Collections.unmodifiableList(all);
    }

    private static String[] effectNames() {
        return new String[] { "Dust", "Crit", "Magic", "Star", "Fire", "Soul", "Spark",
                "Heart", "Note", "Snow", "Glow", "Totem", "Rune", "Happy" };
    }

    private static String[] color2Values() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 1];
        a[0] = "OFF";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, n);
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
