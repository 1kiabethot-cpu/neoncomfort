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

        /** правый клик: предыдущее значение (у переключателя то же, что и обычный клик) */
        public void back() {
            if (isOption()) index = (index - 1 + values.length) % values.length;
            else enabled = !enabled;
        }

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
            "Sunset", "Pink", "Aurora", "Violet", "Gold", "Toxic", "Polar", "Nebula");
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
            "OFF", "Void", "Gold", "Ember", "Frost", "Rose", "Feather", "Dragon", "Bat", "Butterfly", "Crystal",
            "Raven", "Jade", "Inferno", "Seraph", "Demon", "Spirit");
    public static final Module HAT = Module.option("Hat", true,
            "OFF", "Crown", "Halo", "Top Hat", "Cat Ears", "Horns", "Wizard", "China", "Antlers", "Bunny", "Headphones", "Pumpkin");
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
    public static final Module PRESET = Module.option("Preset", false, "Custom", "Destra", "Neon", "Minimal", "PvP", "Mine", "Blood Moon");
    /** Кнопки-действия: выбор пресета ничего не меняет, пока не нажата Apply Preset; Undo Preset возвращает прошлые настройки */
    public static final Module PRESET_APPLY = Module.option("Apply Preset", false, "Tap");
    public static final Module PRESET_UNDO = Module.option("Undo Preset", false, "Tap");
    public static final Module ORBIT = Module.option("Orbit Blades", true, "OFF", "Few", "Normal", "Many");
    public static final Module KILL_LIGHTNING = Module.option("Kill Lightning", true, "OFF", "Soft", "Strong");
    public static final Module LIGHTNING_COLOR = Module.option("Lightning Color", false, menuPlus());
    public static final Module COMPASS = new Module("Compass Bar", false);

    // --- версия 2.3: анимация меню (OFF / Fast / Smooth), по умолчанию Smooth ---
    public static final Module MENU_ANIM = Module.option("Menu Animation", false, "OFF", "Fast", "Smooth");
    static {
        MENU_ANIM.index = 2;     // сохранённый конфиг это перекроет
    }

    // --- версия 2.4: свои цвета для кубов, клинков и зрезов (Aura = как у ауры) и смешивание двух цветов ---
    public static final Module CUBES_COLOR = Module.option("Cubes Color", false, auraPlus());
    public static final Module CUBES_COLOR2 = Module.option("Cubes Mix", true, color2Values());
    public static final Module ORBIT_COLOR = Module.option("Orbit Color", false, auraPlus());
    public static final Module ORBIT_COLOR2 = Module.option("Orbit Mix", true, color2Values());
    public static final Module SLASH_COLOR2 = Module.option("Slash Mix", true, color2Values());
    // --- версия 2.6: свой цвет и смешивание для неоновых крыльев ---
    public static final Module WING_COLOR = Module.option("Wing Color", false, auraPlus());
    public static final Module WING_COLOR2 = Module.option("Wing Mix", true, color2Values());

    // --- версия 2.5: питомцы (клиентские, видны только вам) ---
    public static final Module PET = Module.option("Pet", true, "OFF", "Panda", "Frog", "Sniffer", "Bee", "Ocelot", "Cat", "Fox", "Wolf", "Rabbit", "Parrot", "Axolotl", "Allay");
    public static final Module PET_SIZE = Module.option("Pet Size", false, "Small", "Normal", "Big");
    static {
        PET_SIZE.index = 1;
    }

    // --- версия 2.13: небо в мире и безопасный режим без мерцания ---
    public static final Module SKY_MODE = Module.option("Sky Mode", true, "OFF", "Cosmos", "Blood Moon", "Aurora", "Synthwave", "Nebula", "Sunset");
    public static final Module SKY_POWER = Module.option("Sky Power", false, "Soft", "Normal", "Strong");
    /** Comfort Mode: убирает мигание и резкие пульсации (звёзды, рамка хотбара, предупреждения) */
    public static final Module COMFORT = new Module("Comfort Mode", false);
    static {
        SKY_POWER.index = 1;
        COMFORT.enabled = true;
    }

    // --- версия 2.14: 3D-визуалы мира (рисуются в мире, не поверх экрана; см. WorldVisuals) ---
    public static final Module SKY_STARS = Module.option("Sky Stars", true, "OFF", "Few", "Normal", "Many");
    public static final Module SKY_STAR_COLOR = Module.option("Sky Stars Color", false, autoValues());
    public static final Module SHOOTING_STARS = Module.option("Shooting Stars", true, "OFF", "Rare", "Often");
    public static final Module SKY_BODY = Module.option("Sky Body", true, "OFF", "Giant Moon", "Blood Moon", "Neon Moon", "Ringed Planet", "Twin Moons", "Eclipse", "Supermoon");
    public static final Module RAINBOW = Module.option("Rainbow", true, "OFF", "Soft", "Vivid");
    public static final Module SUN_GLOW = Module.option("Sun Glow", true, "OFF", "Soft", "Strong");
    public static final Module BLOCK_GLOW = Module.option("Block Glow", true, "OFF", "Outline", "Fill", "Both");
    public static final Module BLOCK_COLOR = Module.option("Block Color", false, menuPlus());
    public static final Module BLOCK_COLOR2 = Module.option("Block Mix", true, color2Values());
    public static final Module AIR_FX = Module.option("Air FX", true, "OFF", "Fireflies", "Petals", "Snow", "Embers", "Ash", "Spores");
    // --- версия 2.15: пори року и Light Shader (без нагрузки на FPS) ---
    public static final Module SEASON = Module.option("Season", true, "OFF", "Spring", "Summer", "Autumn", "Winter", "Cycle");
    public static final Module SEASON_PARTS = new Module("Season Particles", true);
    public static final Module LIGHT_SHADER = Module.option("Light Shader", true,
            "OFF", "Soft", "Vivid", "Cinematic", "Neon", "Moody", "Golden", "Frost", "Blood");
    // --- версия 2.17: звуки меню и цветной дождь ---
    public static final Module MENU_SOUND = Module.option("Menu Sound", false, "OFF", "Click", "Soft", "Pop", "Chime", "Crystal");
    static {
        MENU_SOUND.index = 1;
    }
    public static final Module RAIN_FX = Module.option("Rain FX", true, "OFF", "Blood Rain", "Ember Rain", "Neon Rain", "Toxic Rain", "Ash Fall");
    public static final Module RAIN_AMOUNT = Module.option("Rain Amount", false, "Medium", "Less", "More");
    public static final Module RAIN_WHEN = Module.option("Rain When", false, "Always", "Only in Rain");
    public static final Module RAIN_COLOR = Module.option("Rain Color", false, autoValues());
    public static final Module RAIN_TINT = new Module("Rain Tint", true);
    // настоящая окраска мира (через миксины): небо / облака / туман и сам ванильный дождь
    public static final Module SKY_TINT = Module.option("Sky Tint", true, "OFF", "Blood", "Sunset", "Violet", "Toxic", "Ice", "Gold", "Ocean", "Season");
    public static final Module SKY_TINT_POWER = Module.option("Sky Tint Power", false, "Medium", "Light", "Strong");
    public static final Module RAIN_REAL = Module.option("Rain Real Color", true, color2Values());
    public static final Module AIR_AMOUNT = Module.option("Air FX Amount", false, "Medium", "Less", "More");
    /** Look: готовый набор визуалов. Выбор ничего не меняет, пока не нажата Apply Look (отменяется кнопкой Undo Preset) */
    public static final Module LOOK = Module.option("Visual Look", false, "Custom", "Cinematic Night", "Fantasy", "Golden Hour", "Winter Magic", "Blood Night", "Blood Storm");
    public static final Module LOOK_APPLY = Module.option("Apply Look", false, "Tap");

    // --- версия 2.12: вид руки, чистка накладок, микшер звука, типы точек ---
    public static final Module VIEW_MODEL = Module.option("View Model", true, "OFF", "Low Left", "Low Right", "Far", "Small", "Big");
    public static final Module OVERLAY_CLEAN = new Module("Overlay Cleaner", false);
    public static final Module WP_TYPE = Module.option("Waypoint Type", false, "Point", "Event", "Base", "Loot", "Danger");
    public static final Module MIX_PLAYERS = Module.option("Mix Players", false, "Default", "Mute", "25%", "50%", "75%", "100%");
    public static final Module MIX_BLOCKS = Module.option("Mix Blocks", false, "Default", "Mute", "25%", "50%", "75%", "100%");
    public static final Module MIX_MOBS = Module.option("Mix Mobs", false, "Default", "Mute", "25%", "50%", "75%", "100%");
    public static final Module MIX_AMBIENT = Module.option("Mix Ambient", false, "Default", "Mute", "25%", "50%", "75%", "100%");
    public static final Module MIX_MUSIC = Module.option("Mix Music", false, "Default", "Mute", "25%", "50%", "75%", "100%");

    // --- версия 2.11: информационные функции HUD ---
    public static final Module STREAMER = new Module("Streamer Mode", false);
    public static final Module ARMOR_WARN = new Module("Armor Warning", false);
    public static final Module ITEM_COUNTER = new Module("Item Counter", false);
    public static final Module INFO_PANEL = new Module("Info Panel", false);
    public static final Module LOW_HP = new Module("Low HP Pulse", false);
    public static final Module BIOME_TITLE = new Module("Biome Title", false);

    // --- версия 2.10: команда, точки, фильтр частиц ---
    public static final Module TEAM_TRACKER = new Module("Team Tracker", false);
    public static final Module TEAM_ADD = Module.option("Add Team Mate", false, "Tap");
    public static final Module TEAM_CLEAR = Module.option("Clear Team", false, "Tap");
    public static final Module WAYPOINTS = new Module("Waypoints", false);
    public static final Module WP_ADD = Module.option("Add Waypoint", false, "Tap");
    public static final Module WP_CLEAR = Module.option("Clear Waypoints", false, "Tap");
    public static final Module WP_SHARE = Module.option("Share Coords", false, "Tap");
    public static final Module PARTICLE_MODE = Module.option("Particle Filter", false, "OFF", "Reduced", "Minimal");

    /** Показывает в ватермарке, сколько времени забирает сам мод (HUD за кадр и обработчики за тик) */
    public static final Module MOD_TIME = new Module("Mod Time", false);

    /** Авто-оптимизация: при низком FPS тяжёлые эффекты упрощаются сами */
    public static final Module AUTO_PERF = new Module("Auto Perf", false);
    static {
        AUTO_PERF.enabled = true;
    }

    // --- версия 2.9: предметы на спине ---
    public static final Module BACK = Module.option("Back Item", true, "OFF", "Backpack", "Quiver", "Katana");

    // --- версия 2.7: эффекты, свой туман, время, метка цели, боковое меню, плащи ---
    public static final Module POTIONS = new Module("Potions HUD", false);
    public static final Module FOG = Module.option("Custom Fog", true, "OFF", "Light", "Medium", "Thick");
    public static final Module FOG_COLOR = Module.option("Fog Color", false, menuPlus());
    public static final Module FOG_COLOR2 = Module.option("Fog Mix", true, color2Values());
    public static final Module TIME = Module.option("Time Changer", true, "OFF", "Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight");
    public static final Module TARGET_MARKER = Module.option("Target Marker", true, "OFF", "Ring", "Crystals", "Both");
    public static final Module TARGET_COLOR = Module.option("Target Color", false, menuPlus());
    public static final Module TARGET_COLOR2 = Module.option("Target Mix", true, color2Values());
    public static final Module MENU_LAYOUT = Module.option("Menu Layout", false, "Grid", "Sidebar");
    public static final Module CAPE = Module.option("Cape", true, "OFF", "Ace", "D20", "Card", "Arcane", "Moon", "Rain", "Snow", "Thunder", "Wind", "Sun", "Fire", "Forest", "Cosmos", "Shadow", "Royal", "Creeper", "Ender", "Neon");
    public static final Module CAPE_COLOR = Module.option("Cape Color", false, auraPlus());
    public static final Module CAPE_COLOR2 = Module.option("Cape Mix", true, color2Values());
    static {
        MENU_LAYOUT.index = 1;
    }

    /** размер неоновой рамки хотбара: Slot = по слоту, Wide / Big = шире (под ресурспаки с большими уголками) */
    public static final Module HOTBAR_FRAME = Module.option("Hotbar Frame", false, "Slot", "Wide", "Big");
    static {
        HOTBAR_FRAME.index = 1;
    }

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

    // ===== страницы меню (боковая панель): Main = всё, что не про визуалы; остальные по темам =====
    public static final List<Module> PAGE1 = List.of(   // Main / Основное
            MENU_THEME, MENU_COLOR, MENU_LAYOUT, MENU_ANIM, MENU_SOUND, HUD_SIZE, PRESET, PRESET_APPLY, PRESET_UNDO, FPS_BOOST, AUTO_PERF, FULLBRIGHT,
            AUTOSPRINT, ZOOM, NO_HURT_CAM, LOW_FIRE, HIT_SOUND, HIT_VOLUME);
    public static final List<Module> PAGE2 = List.of(   // HUD
            WATERMARK, COORDS, ARMOR, PING_TIME, TARGET, NEARBY_ARMOR, BINDS, POTIONS, COMPASS,
            CROSSHAIR, CROSSHAIR_COLOR, CROSSHAIR_SIZE, CROSSHAIR_OUTLINE,
            HOTBAR_GLOW, HOTBAR_COLOR, HOTBAR_COLOR2, HOTBAR_FRAME, HIT_MARKER);
    public static final List<Module> PAGE3 = List.of(   // World / Мир
            WEATHER, TIME, FOG, FOG_COLOR, FOG_COLOR2, TWILIGHT, TWILIGHT_STYLE,
            NEON_STARS, STAR_COLOR, STAR_COLOR2, SCREEN_FX, SCREEN_FX_AMOUNT, SCREEN_FX_COLOR,
            SEASON, SEASON_PARTS, LIGHT_SHADER, SKY_MODE, SKY_BODY);
    public static final List<Module> PAGE4 = List.of(   // Combat / Бой
            HIT_EFFECT, HIT_COLOR, HIT_COLOR2, HIT_AMOUNT, HIT_GLOW, HIT_EFFECT_COLOR, HIT_RING, HIT_RING_SIZE,
            SLASH, SLASH_COLOR, SLASH_COLOR2, KILL_EFFECT, KILL_COLOR, KILL_LIGHTNING, LIGHTNING_COLOR,
            TARGET_MARKER, TARGET_COLOR, TARGET_COLOR2);
    public static final List<Module> PAGE5 = List.of(   // Movement / Движение
            TRAIL, TRAIL_EFFECT, TRAIL_COLOR2, TRAIL_AMOUNT, TRAIL_GLOW, TRAIL_EFFECT_COLOR,
            PROJ_TRAIL, PROJ_COLOR, PROJ_COLOR2, FOOT_GLOW, FOOT_COLOR, FOOT_COLOR2,
            JUMP_CIRCLE, JUMP_COLOR, JUMP_COLOR2);
    public static final List<Module> PAGE6 = List.of(   // Aura / Аура
            AURA_RINGS, AURA_COLOR, AURA_SIZE, AURA_SPEED, AURA_GLOW, AURA_SPARKS,
            CUBES, CUBE_MOTION, CUBE_LOOK, CUBE_SIZE, CUBE_BURST, CUBES_COLOR, CUBES_COLOR2,
            ORBIT, ORBIT_COLOR, ORBIT_COLOR2);
    public static final List<Module> PAGE7 = List.of(   // Cosmetics / Косметика
            WINGS, WING_SIZE, WING_FLAP, WING_COLOR, WING_COLOR2, CAPE, CAPE_COLOR, CAPE_COLOR2,
            TAIL, HAT, BACK, PET, PET_SIZE, MOD_TIME);
    public static final List<Module> PAGE8 = List.of(
            TEAM_TRACKER, TEAM_ADD, TEAM_CLEAR, WAYPOINTS, WP_TYPE, WP_ADD, WP_CLEAR, WP_SHARE, PARTICLE_MODE);
    public static final List<Module> PAGE9 = List.of(STREAMER, ARMOR_WARN, ITEM_COUNTER, INFO_PANEL, LOW_HP, BIOME_TITLE, VIEW_MODEL, OVERLAY_CLEAN);
    public static final List<Module> PAGE10 = List.of(SKY_MODE, SKY_POWER, COMFORT, MIX_PLAYERS, MIX_BLOCKS, MIX_MOBS, MIX_AMBIENT, MIX_MUSIC);
    public static final List<Module> PAGE11 = List.of(   // Visuals / Візуали
            SKY_BODY, SKY_STARS, SKY_STAR_COLOR, SHOOTING_STARS, SUN_GLOW, RAINBOW,
            BLOCK_GLOW, BLOCK_COLOR, BLOCK_COLOR2, AIR_FX, AIR_AMOUNT, SEASON, SEASON_PARTS, LIGHT_SHADER, RAIN_FX, RAIN_AMOUNT, RAIN_WHEN, RAIN_COLOR, RAIN_TINT,
            SKY_TINT, SKY_TINT_POWER, RAIN_REAL, LOOK, LOOK_APPLY);
    /** вкладка Quick в меню: всё для быстрого переключения визуалов (не страница: Lite её не трогает) */
    public static final List<Module> QUICK = List.of(
            SEASON, SEASON_PARTS, LIGHT_SHADER, RAIN_FX, RAIN_WHEN, SKY_TINT, SKY_MODE, SKY_BODY, SKY_STARS, AIR_FX,
            TWILIGHT, FOG, WEATHER, TIME, FULLBRIGHT, FPS_BOOST);
    public static final List<List<Module>> PAGES = List.of(PAGE1, PAGE2, PAGE3, PAGE4, PAGE5, PAGE6, PAGE7, PAGE8, PAGE9, PAGE10, PAGE11);

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
            FPS_BOOST, FULLBRIGHT, ZOOM,
            SEASON, LIGHT_SHADER, SKY_MODE);

    /** все модули (для сохранения конфига) */
    public static final List<Module> ALL;
    static {
        List<Module> all = new java.util.ArrayList<>();
        for (List<Module> pg : PAGES) for (Module m : pg) if (!all.contains(m)) all.add(m);   // один пункт может стоять на нескольких страницах
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

    private static String[] auraPlus() {
        int n = Theme.COLOR_NAMES.length;
        String[] a = new String[n + 2];
        a[0] = "Aura";
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
