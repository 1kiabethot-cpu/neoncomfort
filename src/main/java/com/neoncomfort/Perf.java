package com.neoncomfort;

import net.minecraft.client.MinecraftClient;

/**
 * Авто-оптимизация: если FPS долго низкий (меньше 30 около 2 секунд), тяжёлые эффекты упрощаются
 * (меньше частиц, звёзд и полос неба). Когда FPS снова выше 45, всё возвращается.
 */
public final class Perf {
    private static boolean low = false;
    private static int cnt = 0;

    public static void tick(MinecraftClient mc) {
        if (!Modules.AUTO_PERF.enabled) {
            low = false;
            cnt = 0;
            return;
        }
        int fps = mc.getCurrentFps();
        if (!low) {
            cnt = fps < 30 ? cnt + 1 : 0;
            if (cnt >= 40) { low = true; cnt = 0; }
        } else {
            cnt = fps > 45 ? cnt + 1 : 0;
            if (cnt >= 60) { low = false; cnt = 0; }
        }
    }

    public static boolean reduced() { return low; }

    // --- профайлер: сколько времени забирает сам мод (скользящее среднее) ---
    private static double hudMs = 0.0;
    private static double tickMs = 0.0;
    private static long tickT0 = 0L;

    public static void tickStart() { tickT0 = System.nanoTime(); }

    public static void tickEnd() {
        double ms = (System.nanoTime() - tickT0) / 1.0e6;
        tickMs = tickMs * 0.95 + ms * 0.05;
    }

    public static void addHud(long ns) { hudMs = hudMs * 0.97 + (ns / 1.0e6) * 0.03; }

    public static String report() {
        return "HUD " + (Math.round(hudMs * 100.0) / 100.0) + " | tick " + (Math.round(tickMs * 100.0) / 100.0) + " ms";
    }

    private Perf() {}
}
