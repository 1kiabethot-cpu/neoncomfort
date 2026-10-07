package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;

/** FPS Boost: лёгкая замена части настроек OptiFine (без шейдеров). */
public final class Performance {
    private static boolean saved = false;
    private static int oView;
    private static CloudRenderMode oClouds;
    private static GraphicsMode oGraphics;
    private static double oEntity;

    /** Применяет текущий уровень Modules.FPS_BOOST: 0 = вернуть как было, 1 = Balanced, 2 = Max. */
    public static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        GameOptions o = mc.options;
        int lvl = Modules.FPS_BOOST.index;

        if (lvl == 0) {
            if (!saved) return;
            o.getViewDistance().setValue(oView);
            o.getCloudRenderMode().setValue(oClouds);
            o.getGraphicsMode().setValue(oGraphics);
            o.getEntityDistanceScaling().setValue(oEntity);
            saved = false;
        } else {
            if (!saved) {
                oView = o.getViewDistance().getValue();
                oClouds = o.getCloudRenderMode().getValue();
                oGraphics = o.getGraphicsMode().getValue();
                oEntity = o.getEntityDistanceScaling().getValue();
                saved = true;
            }
            int maxView = lvl == 1 ? 8 : 6;
            o.getViewDistance().setValue(Math.min(oView, maxView));
            o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            o.getGraphicsMode().setValue(GraphicsMode.FAST);
            o.getEntityDistanceScaling().setValue(lvl == 1 ? Math.min(oEntity, 0.75) : Math.min(oEntity, 0.5));
        }
        o.write();
    }

    private Performance() {}
}
