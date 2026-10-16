package com.neoncomfort;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Неоновые крылья из геометрии (а не из плоской текстуры): пёрышки, перепонки, кристаллы.
 * Цвет берётся из Wing Color / Wing Mix и плавно переходит от основания крыла к кончикам.
 * Рисуются слоем Eyes: светятся без теней.
 */
final class NeonWings {
    /** Контекст рисования одного крыла: преобразует точки крыла (x вправо, y вниз) в систему тела. */
    private static final class Ctx {
        VertexConsumer vc;
        Matrix4f mat;
        float sg;          // +1 левое крыло, -1 правое (зеркало)
        float cos, sin;    // отклонение крыла назад (взмах)
        float size;
        float alpha;

        void vtx(float x, float y, float u, float a) {
            float lx = x * size, ly = y * size;
            float bx = sg * (1f + lx * cos);
            float bz = 2.4f + lx * sin;
            float by = -8f + ly;
            int rgb = Cosmetics.fxColor(Modules.WING_COLOR, Modules.WING_COLOR2, true, u, 0L);
            int al = (int) Math.max(0f, Math.min(255f, 255f * a * alpha));
            Cosmetics.tv(vc, mat, bx, by, bz, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, al);
        }

        /** четырёхугольник, видимый с обеих сторон */
        void quad(float x0, float y0, float u0, float a0, float x1, float y1, float u1, float a1,
                  float x2, float y2, float u2, float a2, float x3, float y3, float u3, float a3) {
            vtx(x0, y0, u0, a0);
            vtx(x1, y1, u1, a1);
            vtx(x2, y2, u2, a2);
            vtx(x3, y3, u3, a3);
            vtx(x3, y3, u3, a3);
            vtx(x2, y2, u2, a2);
            vtx(x1, y1, u1, a1);
            vtx(x0, y0, u0, a0);
        }

        /** многоугольник веером от первой точки; p[i] = {x, y, u} */
        void fan(float[][] p, float a) {
            for (int k = 1; k + 1 < p.length; k++) {
                quad(p[0][0], p[0][1], p[0][2], a, p[k][0], p[k][1], p[k][2], a,
                     p[k + 1][0], p[k + 1][1], p[k + 1][2], a, p[k + 1][0], p[k + 1][1], p[k + 1][2], a);
            }
        }

        /** тонкая полоска от точки к точке (кость, жилка, стержень пера) */
        void bar(float x0, float y0, float u0, float x1, float y1, float u1, float w, float a) {
            float dx = x1 - x0, dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1.0e-4f) return;
            float px = -dy / len * w * 0.5f, py = dx / len * w * 0.5f;
            quad(x0 + px, y0 + py, u0, a, x1 + px, y1 + py, u1, a, x1 - px, y1 - py, u1, a, x0 - px, y0 - py, u0, a);
        }
    }

    /** style: 1 Feather, 2 Dragon, 3 Bat, 4 Butterfly, 5 Crystal. Матрица m уже в системе тела. */
    static void render(MatrixStack m, VertexConsumerProvider vcp, int style, float age, float flap, float flapSpeed, float size) {
        Ctx c = new Ctx();
        c.mat = m.peek().getPositionMatrix();
        c.vc = vcp.getBuffer(RenderLayer.getEyes(Cosmetics.TEX));
        float sweep = 0.30f + flap;
        c.cos = (float) Math.cos(sweep);
        c.sin = (float) Math.sin(sweep);
        for (int pass = 0; pass < 2; pass++) {              // 0 = мягкое свечение, 1 = ядро
            c.size = size * (pass == 0 ? 1.10f : 1.0f);
            c.alpha = pass == 0 ? 0.22f : 1.0f;
            for (int side = 0; side < 2; side++) {
                c.sg = side == 0 ? 1f : -1f;
                switch (style) {
                    case 1: feather(c, age, flapSpeed); break;
                    case 2: membrane(c, age, flapSpeed, false); break;
                    case 3: membrane(c, age, flapSpeed, true); break;
                    case 4: butterfly(c, age, flapSpeed); break;
                    case 5: crystal(c, age); break;
                    default: break;
                }
            }
        }
    }

    /** Ангельские перья: два ряда, каждое перо чуть колышется со сдвигом фазы. */
    private static void feather(Ctx c, float age, float fs) {
        int n = 9;
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                float ang = -0.25f + 1.6f * t + (float) Math.sin(age * 0.15f * fs - i * 0.4f) * 0.05f;
                if (row == 1) ang += 0.08f;
                float len = row == 0 ? 26f - 9f * t : 15f - 5f * t;
                float rx = 0.5f, ry = 1.0f + i * 0.5f;
                float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
                float px = -dy, py = dx;
                float mx = rx + dx * len * 0.45f, my = ry + dy * len * 0.45f;
                float tx = rx + dx * len, ty = ry + dy * len;
                float w = row == 0 ? 2.4f : 2.0f;
                float a = row == 0 ? 0.95f : 0.55f;
                c.quad(rx, ry, 0f, a, mx + px * w, my + py * w, 0.45f, a, tx, ty, 1f, a * 0.5f, mx - px * w, my - py * w, 0.45f, a);
                c.bar(rx, ry, 0f, tx, ty, 1f, 0.6f, a);
            }
        }
    }

    /** Дракон (4 пальца) и летучая мышь (3 пальца): кости и перепонка с вогнутым нижним краем. */
    private static void membrane(Ctx c, float age, float fs, boolean bat) {
        float sw = (float) Math.sin(age * 0.15f * fs) * 1.2f;
        float[][] tips = bat
                ? new float[][] { { 25f, 0f + sw }, { 20f, 13f + sw }, { 9f, 20f } }
                : new float[][] { { 26f, -2f + sw }, { 24f, 8f + sw }, { 19f, 17f + sw * 0.5f }, { 11f, 22f } };
        float ex = 9f, ey = -4f;                                 // локоть
        float cave = bat ? 0.38f : 0.25f;                        // насколько вогнут нижний край
        for (int i = 0; i + 1 < tips.length; i++) {
            float mx = (tips[i][0] + tips[i + 1][0]) * 0.5f;
            float my = (tips[i][1] + tips[i + 1][1]) * 0.5f;
            mx += (ex - mx) * cave;
            my += (ey - my) * cave;
            c.quad(0f, 1f, 0f, 0.5f, tips[i][0], tips[i][1], 1f, 0.35f, mx, my, 0.75f, 0.45f, tips[i + 1][0], tips[i + 1][1], 1f, 0.35f);
        }
        c.bar(0f, 1f, 0f, ex, ey, 0.3f, 1.3f, 1f);
        for (float[] t : tips) c.bar(ex, ey, 0.3f, t[0], t[1], 1f, 0.9f, 1f);
    }

    /** Бабочка: два крупных лепестка, жилки и яркие пятна. */
    private static void butterfly(Ctx c, float age, float fs) {
        float f = 1f + (float) Math.sin(age * 0.15f * fs) * 0.04f;
        float[][] up = { { 0f, 2f, 0f }, { 8f * f, -8f, 0.4f }, { 20f * f, -9f, 0.8f }, { 26f * f, -2f, 1f },
                         { 22f * f, 7f, 0.8f }, { 12f * f, 9f, 0.5f }, { 0f, 6f, 0f } };
        float[][] lo = { { 0f, 6f, 0f }, { 12f * f, 9f, 0.5f }, { 19f * f, 14f, 0.8f }, { 18f * f, 22f, 0.9f },
                         { 10f * f, 24f, 0.7f }, { 4f * f, 16f, 0.3f } };
        c.fan(up, 0.6f);
        c.fan(lo, 0.55f);
        float[][] veins = { { 8f, -8f }, { 20f, -9f }, { 26f, -2f }, { 22f, 7f } };
        for (float[] v : veins) c.bar(0f, 3f, 0f, v[0] * f, v[1], 1f, 0.7f, 1f);
        float[][] spots = { { 17f, -3f }, { 21f, 2f }, { 13f, 17f } };
        for (float[] s : spots) {
            float x = s[0] * f, y = s[1];
            c.quad(x - 1.4f, y - 1.4f, 1f, 1f, x + 1.4f, y - 1.4f, 1f, 1f, x + 1.4f, y + 1.4f, 1f, 1f, x - 1.4f, y + 1.4f, 1f, 1f);
        }
    }

    /** Кристаллы: шесть острых осколков с яркой внутренней гранью, мягко пульсируют. */
    private static void crystal(Ctx c, float age) {
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.08f);
        int n = 6;
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            float ang = -0.2f + 1.3f * t;
            float len = 25f - 8f * t;
            float rx = 0.5f, ry = 1f + i * 2f;
            float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
            float px = -dy, py = dx;
            float sx = rx + dx * len * 0.35f, sy = ry + dy * len * 0.35f;
            float tx = rx + dx * len, ty = ry + dy * len;
            float w = 3.2f;
            float a = 0.45f * pulse;
            c.quad(rx, ry, 0f, a, sx + px * w, sy + py * w, 0.35f, a, tx, ty, 1f, a, sx - px * w, sy - py * w, 0.35f, a);
            float b = 0.9f * pulse;
            c.quad(rx, ry, 0f, b, sx + px * w, sy + py * w, 0.35f, b, tx, ty, 1f, b, sx, sy, 0.5f, b);
        }
    }

    private NeonWings() {}
}
