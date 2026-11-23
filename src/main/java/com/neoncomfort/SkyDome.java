package com.neoncomfort;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Sky Mode: настоящее небо в мире. Вокруг камеры рисуется полусфера с пиксельной текстурой (космос, кровавая луна, полярное сияние),
 * поэтому она стоит на месте при повороте камеры, закрывается горами и домами и не лежит поверх экрана.
 * Никакого мерцания: небо лишь очень медленно дрейфует, а яркость плавно зависит от времени суток (ночью видно, днём нет).
 */
public final class SkyDome {
    private static final Identifier[] TEX = {
        Identifier.of("neoncomfort", "textures/environment/sky_cosmos.png"),
        Identifier.of("neoncomfort", "textures/environment/sky_blood.png"),
        Identifier.of("neoncomfort", "textures/environment/sky_aurora.png"),
        Identifier.of("neoncomfort", "textures/environment/sky_synthwave.png"),
        Identifier.of("neoncomfort", "textures/environment/sky_nebula.png"),
        Identifier.of("neoncomfort", "textures/environment/sky_sunset.png")
    };
    /** насколько небо видно днём (0 = только ночью): Synthwave слегка, Sunset заметно */
    private static final float[] DAY = { 0f, 0f, 0f, 0.20f, 0f, 0.42f };
    private static final int LON = 64;
    private static final int LAT = 24;

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float tv, int a) {
        vc.vertex(m, x, y, z).color(255, 255, 255, a).texture(u, tv)
            .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(0f, 1f, 0f);
    }

    /** дождь гасит дневное небо */
    private static float w0(MinecraftClient mc) { return Math.min(1f, mc.world.getRainGradient(1f) * 1.5f); }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public static void render(WorldRenderContext ctx) {
        int mode = Modules.SKY_MODE.index;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mode <= 0 || mode > TEX.length || mc.world == null) return;
        VertexConsumerProvider vcp = ctx.consumers();
        MatrixStack ms = ctx.matrixStack();
        if (vcp == null || ms == null) return;
        // 0 = день, 1 = ночь: плавно по углу неба (0 = полдень, 0.5 = полночь)
        float sa = mc.world.getSkyAngle(1f);
        float night = smooth((float) (0.5 - Math.cos(sa * Math.PI * 2.0) * 0.9));
        int pw = Modules.SKY_POWER.index;
        float amt = Math.max(night, DAY[mode - 1] * (1f - Math.max(0f, w0(mc)))) * (pw == 0 ? 0.55f : (pw == 1 ? 0.8f : 1.0f));
        if (amt < 0.02f) return;
        float r = Math.max(48f, Math.min(150f, mc.options.getViewDistance().getValue() * 16f * 0.85f));
        float drift = (float) ((System.nanoTime() / 1.0e9) * 0.0015 * Math.PI * 2.0);   // очень медленно, без рывков
        Matrix4f mat = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEyes(TEX[mode - 1]));
        for (int i = 0; i < LAT; i++) {
            float t0 = i / (float) LAT, t1 = (i + 1) / (float) LAT;
            float e0 = (float) Math.toRadians(-6.0 + 96.0 * t0), e1 = (float) Math.toRadians(-6.0 + 96.0 * t1);
            float c0 = (float) Math.cos(e0), s0 = (float) Math.sin(e0), c1 = (float) Math.cos(e1), s1 = (float) Math.sin(e1);
            // у горизонта небо гаснет, чтобы не заливать далёкую землю
            int a0 = (int) (255f * amt * smooth((float) ((Math.toDegrees(e0) - 3.0) / 28.0)));
            int a1 = (int) (255f * amt * smooth((float) ((Math.toDegrees(e1) - 3.0) / 28.0)));
            for (int j = 0; j < LON; j++) {
                float u0 = j / (float) LON, u1 = (j + 1) / (float) LON;
                float az0 = u0 * (float) (Math.PI * 2.0) + drift, az1 = u1 * (float) (Math.PI * 2.0) + drift;
                float x00 = r * c0 * (float) Math.sin(az0), z00 = r * c0 * (float) Math.cos(az0), y0 = r * s0;
                float x01 = r * c0 * (float) Math.sin(az1), z01 = r * c0 * (float) Math.cos(az1);
                float x10 = r * c1 * (float) Math.sin(az0), z10 = r * c1 * (float) Math.cos(az0), y1 = r * s1;
                float x11 = r * c1 * (float) Math.sin(az1), z11 = r * c1 * (float) Math.cos(az1);
                float v0 = 1f - t0, v1 = 1f - t1;
                v(vc, mat, x00, y0, z00, u0, v0, a0);
                v(vc, mat, x01, y0, z01, u1, v0, a0);
                v(vc, mat, x11, y1, z11, u1, v1, a1);
                v(vc, mat, x10, y1, z10, u0, v1, a1);
                v(vc, mat, x10, y1, z10, u0, v1, a1);
                v(vc, mat, x11, y1, z11, u1, v1, a1);
                v(vc, mat, x01, y0, z01, u1, v0, a0);
                v(vc, mat, x00, y0, z00, u0, v0, a0);
            }
        }
    }

    private SkyDome() {}
}
