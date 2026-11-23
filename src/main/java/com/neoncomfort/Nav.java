package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Навигация: Team Tracker (список тиммейтов с расстоянием и стрелкой) и Waypoints (свои точки: база, ивент, место смерти).
 * Мод видит координаты только тех игроков, которых сервер передаёт клиенту (обычно в радиусе 50-120 блоков).
 */
public final class Nav {
    static final class Wp {
        String name;
        double x, y, z;
        String dim;
    }

    static final List<String> team = new ArrayList<>();
    static final List<Wp> wps = new ArrayList<>();
    private static double lx, ly, lz;
    private static boolean wasAlive = true;
    private static int counter = 1;
    static int kills = 0;
    static int deaths = 0;
    static final long sessionStart = System.currentTimeMillis();

    // ---------------------------------------------------------------- действия (кнопки меню)

    /** добавить в команду ближайшего игрока (до 12 блоков) */
    static void addNearest(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        PlayerEntity best = null;
        double bd = 12.0;
        for (PlayerEntity o : mc.world.getPlayers()) {
            if (o == mc.player || o.isSpectator()) continue;
            double d = mc.player.distanceTo(o);
            if (d < bd) { bd = d; best = o; }
        }
        if (best == null) return;
        String n = best.getName().getString();
        if (!team.contains(n)) team.add(n);
        Config.save();
    }

    static void clearTeam() {
        team.clear();
        Config.save();
    }

    static String dimOf(MinecraftClient mc) {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    static void addWaypoint(MinecraftClient mc, String name, double x, double y, double z) {
        if (mc.world == null) return;
        Wp w = new Wp();
        w.name = name.replace('|', ' ').replace(';', ' ');
        w.x = x;
        w.y = y;
        w.z = z;
        w.dim = dimOf(mc);
        wps.add(w);
        while (wps.size() > 12) wps.remove(0);
        Config.save();
    }

    static void addHere(MinecraftClient mc) {
        if (mc.player == null) return;
        String type = Modules.WP_TYPE.values[Modules.WP_TYPE.index];
        addWaypoint(mc, type + " " + (counter++), mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    static void clearWaypoints() {
        wps.clear();
        Config.save();
    }

    /** отправить свои координаты в командный чат (/tm) */
    static void share(MinecraftClient mc) {
        if (mc.player == null) return;
        String msg = "Я тут: " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        mc.player.networkHandler.sendChatCommand("tm " + msg);
    }

    /** цвет точки по типу: Event оранжевый, Base синий, Loot жёлтый, Danger красный, Death серый */
    static int wpColor(String name) {
        if (name.startsWith("Event")) return 0xFFFF9944;
        if (name.startsWith("Base")) return 0xFF66AAFF;
        if (name.startsWith("Danger")) return 0xFFFF5555;
        if (name.startsWith("Death")) return 0xFFAAAAAA;
        return 0xFFFFE066;
    }

    // ---------------------------------------------------------------- тик: точка смерти

    static void tick(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.getHealth() > 0f) {
            lx = p.getX();
            ly = p.getY();
            lz = p.getZ();
            wasAlive = true;
        } else if (wasAlive) {
            wasAlive = false;
            deaths++;
            if (Modules.WAYPOINTS.enabled) addWaypoint(mc, "Death", lx, ly, lz);
        }
    }

    // ---------------------------------------------------------------- геометрия

    /** угол от направления взгляда игрока к точке, градусы: 0 = прямо, + вправо, - влево */
    static float relAngle(ClientPlayerEntity p, double x, double z) {
        double dx = x - p.getX(), dz = z - p.getZ();
        float world = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float d = (world - p.getYaw()) % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    static String arrow(float rel) {
        String[] a = { "^", "^>", ">", "v>", "v", "<v", "<", "<^" };
        int i = Math.round(rel / 45f);
        return a[((i % 8) + 8) % 8];
    }

    static PlayerEntity find(MinecraftClient mc, String name) {
        if (mc.world == null) return null;
        for (PlayerEntity o : mc.world.getPlayers()) {
            if (o.getName().getString().equalsIgnoreCase(name)) return o;
        }
        return null;
    }

    // ---------------------------------------------------------------- хранение в конфиге

    static String saveTeam() { return String.join(",", team); }

    static void loadTeam(String s) {
        team.clear();
        if (s == null || s.trim().isEmpty()) return;
        for (String n : s.split(",")) if (!n.trim().isEmpty()) team.add(n.trim());
    }

    static String saveWps() {
        StringBuilder sb = new StringBuilder();
        for (Wp w : wps) {
            if (sb.length() > 0) sb.append(';');
            sb.append(w.name).append('|').append(w.x).append('|').append(w.y).append('|').append(w.z).append('|').append(w.dim);
        }
        return sb.toString();
    }

    static void loadWps(String s) {
        wps.clear();
        if (s == null || s.trim().isEmpty()) return;
        for (String part : s.split(";")) {
            String[] t = part.split("\\|");
            if (t.length < 5) continue;
            try {
                Wp w = new Wp();
                w.name = t[0];
                w.x = Double.parseDouble(t[1]);
                w.y = Double.parseDouble(t[2]);
                w.z = Double.parseDouble(t[3]);
                w.dim = t[4];
                wps.add(w);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private Nav() {}
}
