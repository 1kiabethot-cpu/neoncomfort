package com.neoncomfort;

import java.util.List;

public final class Modules {
    public static final class Module {
        public final String name;
        public boolean enabled;
        Module(String name, boolean enabled) { this.name = name; this.enabled = enabled; }
        public void toggle() { enabled = !enabled; }
    }

    public static final Module WATERMARK = new Module("Watermark", true);
    public static final Module COORDS    = new Module("Coordinates", true);
    public static final Module ARMOR     = new Module("Armor HUD", true);
    public static final Module PING_TIME = new Module("Ping & Time", true);
    public static final Module TARGET    = new Module("Target HP", true);
    public static final Module FULLBRIGHT = new Module("Fullbright", false);
    public static final Module AUTOSPRINT = new Module("Auto Sprint", false);
    public static final Module ZOOM      = new Module("Zoom (C)", true);

    public static final List<Module> ALL = List.of(
            WATERMARK, COORDS, ARMOR, PING_TIME, TARGET, FULLBRIGHT, AUTOSPRINT, ZOOM);

    private Modules() {}
}
