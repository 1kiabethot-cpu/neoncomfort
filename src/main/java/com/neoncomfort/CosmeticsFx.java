package com.neoncomfort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;

public class CosmeticsFx implements ClientModInitializer {
    static volatile long hitMs = 0L;

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) hitMs = System.currentTimeMillis();
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(CosmeticsFx::tick);
    }

    static float burstAmount() {
        int mode = Modules.CUBE_BURST.index;
        if (mode <= 0 || hitMs == 0L) return 0f;
        long dt = System.currentTimeMillis() - hitMs;
        float s;
        if (dt < 120L) {
            s = dt / 120f;
        } else if (dt < 1100L) {
            float k = 1f - (dt - 120L) / 980f;
            s = k * k;
        } else {
            s = 0f;
        }
        return s * (mode == 2 ? 2.2f : 1.2f);
    }

    private static void tick(MinecraftClient mc) {
        int mode = Modules.AURA_SPARKS.index;
        if (mode <= 0 || mc.player == null || mc.world == null || mc.isPaused()) return;
        int rgb = Cosmetics.auraColor();
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;
        int n = mode == 1 ? 1 : 3;
        for (int i = 0; i < n; i++) {
            double a = Math.random() * Math.PI * 2.0;
            double rad = 0.5 + Math.random() * 0.7;
            double x = mc.player.getX() + Math.cos(a) * rad;
            double z = mc.player.getZ() + Math.sin(a) * rad;
            double y = mc.player.getY() + Math.random() * 1.9;
            Particle p = mc.particleManager.addParticle(ParticleTypes.END_ROD,
                x, y, z, 0.0, 0.02 + Math.random() * 0.02, 0.0);
            if (p != null) {
                p.setColor(r, g, b);
                p.scale(0.9f);
            }
        }
    }
}
