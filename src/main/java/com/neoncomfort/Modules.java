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
    public static final Module CROSSHAIR  = Module.option("Crosshair", true,
            "OFF", "Dot", "Cross", "Plus", "Square");
    public static final Module CROSSHAIR_COLOR = Module.option("Crosshair Color", false, menuPlus());
    public static final Module TRAIL = Module.option("Trail", true, trailValues());
    public static final Module MENU_COLOR = Module.option("Menu Color", false, Theme.COLOR_NAMES);
    public static final Module NO_HURT_CAM = new Module("No Hurt Cam", false);
    public static final Module LOW_FIRE   = new Module("Low Fire", false);
    public static final Module HIT_SOUND  = Module.option("Hit Sound", true,
            "OFF", "Orb", "Bell", "Pling", "Ding", "Levelup");
    public static final Module WEATHER    = Module.option("Weather", true,
            "Default", "Clear", "Rain", "Thunder");

    public static final List<Module> ALL = List.of(
            WATERMARK, COORDS, ARMOR, PING_TIME, TARGET, FULLBRIGHT, AUTOSPRINT, ZOOM, NEARBY_ARMOR,
            CROSSHAIR, CROSSHAIR_COLOR, TRAIL, MENU_COLOR, NO_HURT_CAM, LOW_FIRE, HIT_SOUND, WEATHER);

    private static String[] menuPlus() {
        String[] a = new String[Theme.COLOR_NAMES.length + 1];
        a[0] = "Menu";
        System.arraycopy(Theme.COLOR_NAMES, 0, a, 1, Theme.COLOR_NAMES.length);
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
