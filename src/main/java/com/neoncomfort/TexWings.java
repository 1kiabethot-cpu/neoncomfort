package com.neoncomfort;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Детальные крылья из большой текстуры wings.png (Raven, Jade, Inferno, Seraph, Demon, Spirit).
 * Крыло разбито на 8 столбцов, каждый чуть сильнее отклонён назад, поэтому при взмахе оно изгибается плавной дугой.
 */
final class TexWings {
    static final Identifier WING_TEX = Identifier.of("neoncomfort", "textures/entity/wings.png");

    private static void v(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float u, float tv, int light) {
        vc.vertex(mat, x / 16f, y / 16f, z / 16f).color(255, 255, 255, 255)
            .texture(u / 512f, tv / 512f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);
    }

    /** style: 1..6. Матрица m уже в системе тела. */
    static void render(MatrixStack m, VertexConsumerProvider vcp, int style, float age, float flap, float flapSpeed,
                       float size, int light) {
        int k = style - 1;
        float cu = (k % 3) * 140f + 2f;                  // начало клетки с крылом в атласе (как в рисовальщике)
        float cv = (k / 3) * 116f + 2f;
        final float cw = 136f, ch = 112f;
        float width = 36f * size, height = 30f * size;
        int cols = 8;
        Matrix4f mat = m.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(WING_TEX));
        float ytop = -10f, ybot = ytop + height;
        float[] px = new float[cols + 1];
        float[] pz = new float[cols + 1];
        for (int j = 0; j < cols; j++) {                // положение столбцов: кончик отклоняется сильнее корня
            float u = (j + 0.5f) / cols;
            float th = 0.30f + flap * (0.35f + 1.1f * u)
                    + 0.05f * u * (float) Math.sin(age * 0.15f * flapSpeed + j * 0.4f);
            px[j + 1] = px[j] + (float) Math.cos(th) * (width / cols);
            pz[j + 1] = pz[j] + (float) Math.sin(th) * (width / cols);
        }
        for (int side = 0; side < 2; side++) {
            float sg = side == 0 ? 1f : -1f;
            for (int j = 0; j < cols; j++) {
                float s0 = j / (float) cols, s1 = (j + 1) / (float) cols;
                // левое крыло (sg = +1): корень справа (u = 1), кончик слева; правое — зеркально
                float t0 = sg > 0 ? 1f - s0 : s0, t1 = sg > 0 ? 1f - s1 : s1;
                float u0 = cu + t0 * cw, u1 = cu + t1 * cw;
                float bx0 = sg * (1f + px[j]), bx1 = sg * (1f + px[j + 1]);
                float bz0 = 2.4f + pz[j], bz1 = 2.4f + pz[j + 1];
                v(vc, mat, bx0, ytop, bz0, u0, cv, light);
                v(vc, mat, bx1, ytop, bz1, u1, cv, light);
                v(vc, mat, bx1, ybot, bz1, u1, cv + ch, light);
                v(vc, mat, bx0, ybot, bz0, u0, cv + ch, light);
            }
        }
    }

    private TexWings() {}
}
