package com.neoncomfort;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 3D-визуалы мира (как в «визуалах» клиентов и на видео): настоящие звёзды, падающие звёзды, гигантская луна / планета с кольцом,
 * радуга, ореол и лучи солнца, подсветка блока под прицелом и частицы воздуха (светлячки, лепестки, снег...).
 *
 * Всё рисуется в мире (как SkyDome), а не поверх экрана: стоит на месте при повороте камеры, закрывается горами и домами.
 * Никакого мерцания: свечение плавное, а при включённом Comfort Mode звёзды и подсветка не пульсируют вовсе.
 * Нагрузка маленькая: несколько сотен полупрозрачных четырёхугольников, при низком FPS (Auto Perf) их вдвое меньше.
 */
public final class WorldVisuals {
    private static final float TU = 20.5f / 128f, TV = 120.5f / 256f;   // белый пиксель в cosmetics.png (как у лент в Cosmetics)
    private static final float TAU = (float) (Math.PI * 2.0);

    private static VertexConsumer vc;
    private static Matrix4f mat;

    // ---------------------------------------------------------------- примитивы

    private static void vtx(float x, float y, float z, int rgb, float a) {
        int al = (int) (clamp01(a) * 255f);
        vc.vertex(mat, x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, al).texture(TU, TV)
            .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(0f, 1f, 0f);
    }

    private static final float[] P = new float[12];
    private static final float[] AL = new float[4];
    private static final int[] CO = new int[4];

    private static void set(int i, float x, float y, float z, int rgb, float a) {
        P[i * 3] = x;
        P[i * 3 + 1] = y;
        P[i * 3 + 2] = z;
        CO[i] = rgb;
        AL[i] = a;
    }

    /** четырёхугольник, видимый с двух сторон (слой Eyes без отсечения граней не гарантирован) */
    private static void emit() {
        for (int i = 0; i < 4; i++) vtx(P[i * 3], P[i * 3 + 1], P[i * 3 + 2], CO[i], AL[i]);
        for (int i = 3; i >= 0; i--) vtx(P[i * 3], P[i * 3 + 1], P[i * 3 + 2], CO[i], AL[i]);
    }

    // базис «лицом к камере» для направления на объект
    private static float rX, rY, rZ, uX, uY, uZ;

    private static void basis(float dx, float dy, float dz) {
        float x = dz, z = -dx;                           // right = up(0,1,0) x d
        float l = (float) Math.sqrt(x * x + z * z);
        if (l < 1.0e-4f) {
            x = 1f;
            z = 0f;
            l = 1f;
        }
        rX = x / l;
        rY = 0f;
        rZ = z / l;
        uX = dy * rZ;                                    // up = d x right
        uY = dz * rX - dx * rZ;
        uZ = -dy * rX;
    }

    private static final float[][] CT = new float[129][], ST_ = new float[129][];

    private static void trig(int segs) {
        if (CT[segs] != null) return;
        float[] c = new float[segs + 1], sn = new float[segs + 1];
        for (int k = 0; k <= segs; k++) {
            c[k] = (float) Math.cos(k * TAU / segs);
            sn[k] = (float) Math.sin(k * TAU / segs);
        }
        CT[segs] = c;
        ST_[segs] = sn;
    }

    /** мягкий диск/свечение: от центра (rgbIn, aIn) к краю (rgbOut, aOut) */
    private static void fan(float cx, float cy, float cz, float rad, int rgbIn, float aIn, int rgbOut, float aOut, int segs) {
        if (aIn <= 0.004f && aOut <= 0.004f) return;
        if (segs > 128) segs = 128;
        trig(segs);
        float[] C = CT[segs], S = ST_[segs];
        for (int k = 0; k < segs; k++) {
            float c0 = C[k], s0 = S[k], c1 = C[k + 1], s1 = S[k + 1];
            set(0, cx, cy, cz, rgbIn, aIn);
            set(1, cx + (rX * c0 + uX * s0) * rad, cy + (rY * c0 + uY * s0) * rad, cz + (rZ * c0 + uZ * s0) * rad, rgbOut, aOut);
            set(2, cx + (rX * c1 + uX * s1) * rad, cy + (rY * c1 + uY * s1) * rad, cz + (rZ * c1 + uZ * s1) * rad, rgbOut, aOut);
            set(3, cx + (rX * c1 + uX * s1) * rad, cy + (rY * c1 + uY * s1) * rad, cz + (rZ * c1 + uZ * s1) * rad, rgbOut, aOut);
            emit();
        }
    }

    /** квадрат-точка (для звёзд) */
    private static void dot(float cx, float cy, float cz, float h, int rgb, float a) {
        set(0, cx - rX * h - uX * h, cy - rY * h - uY * h, cz - rZ * h - uZ * h, rgb, a);
        set(1, cx + rX * h - uX * h, cy + rY * h - uY * h, cz + rZ * h - uZ * h, rgb, a);
        set(2, cx + rX * h + uX * h, cy + rY * h + uY * h, cz + rZ * h + uZ * h, rgb, a);
        set(3, cx - rX * h + uX * h, cy - rY * h + uY * h, cz - rZ * h + uZ * h, rgb, a);
        emit();
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }
    private static float frac(float v) { return v - (float) Math.floor(v); }
    private static float tanDeg(float deg) { return (float) Math.tan(Math.toRadians(deg)); }
    private static int mix(int a, int b, float u) { return FxColor.lerp(a, b, clamp01(u)); }

    /** псевдослучайное число 0..1 из номера «отрезка времени» и ключа (без Random, поэтому всё детерминировано) */
    private static float hash(long b, int k) {
        long x = b * 0x9E3779B97F4A7C15L + k * 0xBF58476D1CE4E5B9L;
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    // ---------------------------------------------------------------- главный рендер

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) return;
        boolean skyOn = Modules.SKY_STARS.index > 0 || Modules.SHOOTING_STARS.index > 0 || Modules.SKY_BODY.index > 0
                || Modules.RAINBOW.index > 0 || Modules.SUN_GLOW.index > 0;
        boolean blockOn = Modules.BLOCK_GLOW.index > 0;
        if (!skyOn && !blockOn) return;
        VertexConsumerProvider vcp = ctx.consumers();
        MatrixStack ms = ctx.matrixStack();
        if (vcp == null || ms == null) return;
        mat = ms.peek().getPositionMatrix();
        vc = vcp.getBuffer(RenderLayer.getEyes(Cosmetics.TEX));
        long now = System.currentTimeMillis();

        if (skyOn && w.getRegistryKey() == World.OVERWORLD) {
            float sa = w.getSkyAngle(1f);
            float th = sa * TAU;                                          // 0 = полдень, PI = полночь
            float night = smooth((float) (0.5 - Math.cos(sa * Math.PI * 2.0) * 0.9));
            float clear = 1f - clamp01(w.getRainGradient(1f));
            float r = Math.max(48f, Math.min(150f, mc.options.getViewDistance().getValue() * 16f * 0.85f));
            if (Modules.SUN_GLOW.index > 0) sunGlow(th, clear, r, now);
            if (Modules.RAINBOW.index > 0) rainbow(th, clear, r);
            if (Modules.SKY_BODY.index > 0) body(Modules.SKY_BODY.index, th, night, clear, r);
            if (Modules.SKY_STARS.index > 0) stars(th, night, clear, r, now);
            if (Modules.SHOOTING_STARS.index > 0) shooting(night, clear, r, now);
        }
        if (blockOn) blockGlow(ctx, mc, w, now);
    }

    // ---------------------------------------------------------------- звёзды

    private static final int MAXS = 420;
    private static final float[] SX = new float[MAXS], SY = new float[MAXS], SZ = new float[MAXS];
    private static final float[] SS = new float[MAXS], ST = new float[MAXS];
    static {
        for (int i = 0; i < MAXS; i++) {
            float u = frac(i * 0.61803399f + 0.137f), v = frac(i * 0.75487767f + 0.311f);
            float y = 1f - 2f * u;
            float rr = (float) Math.sqrt(Math.max(0f, 1f - y * y));
            float ph = v * TAU;
            SX[i] = rr * (float) Math.cos(ph);
            SY[i] = y;
            SZ[i] = rr * (float) Math.sin(ph);
            SS[i] = frac(i * 0.38196601f + 0.5f);
            ST[i] = frac(i * 0.41421356f);
        }
    }

    private static int starRgb(int i, long now) {
        int m = Modules.SKY_STAR_COLOR.index;
        if (m <= 0) {                                                      // Auto: естественные цвета звёзд
            float t = ST[i];
            return t < 0.55f ? mix(0xFFFFFF, 0xCFE0FF, t / 0.55f) : mix(0xFFFFFF, 0xFFE2B8, (t - 0.55f) / 0.45f);
        }
        if (m > Theme.colorCount()) return Theme.rainbowRgb(i * 70L) & 0xFFFFFF;
        return Theme.colorRgb(m - 1) & 0xFFFFFF;
    }

    private static void stars(float th, float night, float clear, float r, long now) {
        float base = night * clear;
        if (base < 0.03f) return;
        int n = new int[] { 0, 140, 260, 420 }[Modules.SKY_STARS.index];
        if (Perf.reduced()) n /= 2;
        float ct = (float) Math.cos(th), st = (float) Math.sin(th);
        boolean calm = Modules.COMFORT.enabled;
        net.minecraft.util.math.Vec3d lk = MinecraftClient.getInstance().player.getRotationVec(1f);
        for (int i = 0; i < n; i++) {
            float x = SX[i] * ct - SY[i] * st;                              // небо вращается вокруг оси Z, как в игре
            float y = SX[i] * st + SY[i] * ct;
            float z = SZ[i];
            if (y < 0.06f) continue;
            if (x * lk.x + y * lk.y + z * lk.z < 0.15f) continue;            // звезда далеко за пределами экрана
            float a = base * smooth((y - 0.06f) / 0.25f) * (0.45f + 0.55f * SS[i]);
            if (!calm) a *= 0.82f + 0.18f * (float) Math.sin(now / 700.0 + i * 1.7);
            if (a < 0.03f) continue;
            basis(x, y, z);
            float h = r * (0.0013f + 0.0022f * SS[i]);
            int rgb = starRgb(i, now);
            float cx = x * r, cy = y * r, cz = z * r;
            dot(cx, cy, cz, h, rgb, a);
            if (SS[i] > 0.82f) {
                fan(cx, cy, cz, h * 5f, rgb, a * 0.30f, rgb, 0f, 8);
                float len = h * (8f + 10f * SS[i]), wd = h * 0.35f;     // яскраві зорі з променями-хрестом
                set(0, cx - rX * len, cy - rY * len, cz - rZ * len, rgb, 0f);
                set(1, cx + uX * wd, cy + uY * wd, cz + uZ * wd, rgb, a * 0.9f);
                set(2, cx + rX * len, cy + rY * len, cz + rZ * len, rgb, 0f);
                set(3, cx - uX * wd, cy - uY * wd, cz - uZ * wd, rgb, a * 0.9f);
                emit();
                set(0, cx - uX * len, cy - uY * len, cz - uZ * len, rgb, 0f);
                set(1, cx + rX * wd, cy + rY * wd, cz + rZ * wd, rgb, a * 0.9f);
                set(2, cx + uX * len, cy + uY * len, cz + uZ * len, rgb, 0f);
                set(3, cx - rX * wd, cy - rY * wd, cz - rZ * wd, rgb, a * 0.9f);
                emit();
            }
        }
    }

    /** падающая звезда: раз в 6 (Often) или 14 (Rare) секунд короткий след по небу */
    private static void shooting(float night, float clear, float r, long now) {
        float base = night * clear;
        if (base < 0.4f) return;
        long interval = Modules.SHOOTING_STARS.index == 1 ? 14000L : 6000L;
        long b = now / interval;
        long off = now - b * interval;
        long dur = 1000L;
        if (off >= dur || hash(b, 9) > 0.85f) return;
        float t = off / (float) dur;
        float az = hash(b, 1) * TAU, el = (float) Math.toRadians(35.0 + hash(b, 2) * 35.0);
        float dx = (float) (Math.cos(el) * Math.sin(az)), dy = (float) Math.sin(el), dz = (float) (Math.cos(el) * Math.cos(az));
        basis(dx, dy, dz);
        float hd = hash(b, 3) * TAU;
        float tx = rX * (float) Math.cos(hd) + uX * (float) Math.sin(hd);
        float ty = rY * (float) Math.cos(hd) + uY * (float) Math.sin(hd);
        float tz = rZ * (float) Math.cos(hd) + uZ * (float) Math.sin(hd);
        float len = 0.55f, tail = 0.20f;
        float head = len * t, tl = Math.max(0f, head - tail);
        float fade = (float) Math.sin(Math.PI * t) * base;
        int segs = 8;
        float w0 = r * 0.0045f;
        // боковой вектор для ширины следа (перпендикулярно и направлению, и тангенте)
        float sx = dy * tz - dz * ty, sy = dz * tx - dx * tz, sz = dx * ty - dy * tx;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1.0e-4f) return;
        sx /= sl;
        sy /= sl;
        sz /= sl;
        float px = 0, py = 0, pz = 0;
        for (int k = 0; k <= segs; k++) {
            float s = tl + (head - tl) * k / segs;
            float cs = (float) Math.cos(s), sn = (float) Math.sin(s);
            float qx = dx * cs + tx * sn, qy = dy * cs + ty * sn, qz = dz * cs + tz * sn;
            if (qy < 0.04f) return;                                       // ушла за горизонт
            float qa = fade * (k / (float) segs);
            if (k > 0) {
                float wk0 = w0 * ((k - 1) / (float) segs), wk1 = w0 * (k / (float) segs);
                float pa = fade * ((k - 1) / (float) segs);
                set(0, px * r - sx * wk0, py * r - sy * wk0, pz * r - sz * wk0, 0xDFF3FF, pa);
                set(1, px * r + sx * wk0, py * r + sy * wk0, pz * r + sz * wk0, 0xDFF3FF, pa);
                set(2, qx * r + sx * wk1, qy * r + sy * wk1, qz * r + sz * wk1, 0xFFFFFF, qa);
                set(3, qx * r - sx * wk1, qy * r - sy * wk1, qz * r - sz * wk1, 0xFFFFFF, qa);
                emit();
            }
            px = qx;
            py = qy;
            pz = qz;
            if (k == segs) {                                              // яркая голова
                basis(qx, qy, qz);
                fan(qx * r, qy * r, qz * r, r * 0.012f, 0xFFFFFF, fade * 0.9f, 0xBFE4FF, 0f, 10);
            }
        }
    }

    // ---------------------------------------------------------------- небесные тела

    private static void body(int mode, float th, float night, float clear, float r) {
        float mdx = (float) Math.sin(th), mdy = -(float) Math.cos(th);   // настоящая луна (противоположна солнцу)
        if (mode != 4) {
            if (mdy < -0.02f) return;
            float a = clear * smooth((mdy + 0.02f) / 0.22f) * (0.30f + 0.70f * night);
            if (a < 0.02f) return;
            if (mode == 1 || mode == 5) moon(mdx, mdy, 0f, 11f, 0xF4F7FF, 0xB4C0E0, 0x9FB4FF, a, r);
            else if (mode == 2) moon(mdx, mdy, 0f, 11f, 0xE63030, 0x8A0E0E, 0xFF1A1A, a, r);
            else if (mode == 3) {
                int ac = Theme.accent() & 0xFFFFFF;
                moon(mdx, mdy, 0f, 11f, mix(ac, 0xFFFFFF, 0.35f), mix(ac, 0x000000, 0.35f), ac, a, r);
            }
            else if (mode == 6) {                                         // Eclipse: тёмно-медная луна с горящим ободом
                basis(mdx, mdy, 0f);
                float ex = mdx * r, ey = mdy * r, rad = r * tanDeg(11f);
                fan(ex, ey, 0f, rad * 3.4f, 0xFF7A2A, a * 0.14f, 0xFF7A2A, 0f, 24);
                moon(mdx, mdy, 0f, 11f, 0x6E1E10, 0x2E0806, 0xFF5A1A, a * 0.8f, r);
                fan(ex, ey, 0f, rad * 1.22f, 0xFFB060, 0f, 0xFFB060, a * 0.55f, 40);
            } else if (mode == 7) moon(mdx, mdy, 0f, 21f, 0xFFEBC4, 0xD9A66A, 0xFFB868, a, r);   // Supermoon: огромная тёплая
            if (mode == 5) {                                              // вторая, маленькая розово-фиолетовая луна
                float t2 = th + 0.62f;
                float x2 = (float) Math.sin(t2), y2 = -(float) Math.cos(t2);
                if (y2 > -0.02f) {
                    float a2 = clear * smooth((y2 + 0.02f) / 0.22f) * (0.30f + 0.70f * night);
                    moon(x2, y2, 0f, 5.5f, 0xF6D2FF, 0xB07CD8, 0xD08CFF, a2, r);
                }
            }
        } else {
            // планета с кольцом: закреплена на небесной сфере и медленно движется вместе с небом
            float bx = 0.55f, by = 0.62f, bz = -0.56f;
            float bl = (float) Math.sqrt(bx * bx + by * by + bz * bz);
            bx /= bl;
            by /= bl;
            bz /= bl;
            float ct = (float) Math.cos(th), st = (float) Math.sin(th);
            float x = bx * ct - by * st, y = bx * st + by * ct, z = bz;
            if (y < 0.04f) return;
            float a = clear * smooth((y - 0.04f) / 0.25f) * (0.25f + 0.75f * night);
            if (a < 0.02f) return;
            planet(x, y, z, 7f, a, r);
        }
    }

    private static void moon(float dx, float dy, float dz, float deg, int hi, int lo, int halo, float a, float r) {
        if (a < 0.02f) return;
        basis(dx, dy, dz);
        float cx = dx * r, cy = dy * r, cz = dz * r;
        float rad = r * tanDeg(deg);
        fan(cx, cy, cz, rad * 3.2f, halo, a * 0.16f, halo, 0f, 24);
        fan(cx, cy, cz, rad * 1.7f, halo, a * 0.30f, halo, 0f, 24);
        fan(cx, cy, cz, rad, hi, a * 0.80f, lo, a * 0.62f, 32);
        fan(cx - rX * rad * 0.22f + uX * rad * 0.20f, cy - rY * rad * 0.22f + uY * rad * 0.20f,
                cz - rZ * rad * 0.22f + uZ * rad * 0.20f, rad * 0.60f, hi, a * 0.30f, hi, 0f, 24);
    }

    private static void planet(float dx, float dy, float dz, float deg, float a, float r) {
        basis(dx, dy, dz);
        float cx = dx * r, cy = dy * r, cz = dz * r;
        float rad = r * tanDeg(deg);
        fan(cx, cy, cz, rad * 2.8f, 0xB07CFF, a * 0.14f, 0xB07CFF, 0f, 24);
        fan(cx, cy, cz, rad, 0x9C86FF, a * 0.85f, 0x4B34A8, a * 0.70f, 32);
        fan(cx - rX * rad * 0.25f + uX * rad * 0.22f, cy - rY * rad * 0.25f + uY * rad * 0.22f,
                cz - rZ * rad * 0.25f + uZ * rad * 0.22f, rad * 0.55f, 0xD8CCFF, a * 0.28f, 0xD8CCFF, 0f, 20);
        // кольцо: наклонённый эллипс, задняя половина скрыта планетой
        int segs = 56;
        float rin = rad * 1.35f, rout = rad * 1.95f, flat = 0.28f;
        float tilt = 0.35f, ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        for (int k = 0; k < segs; k++) {
            float a0 = k * TAU / segs, a1 = (k + 1) * TAU / segs, mid = (a0 + a1) * 0.5f;
            if (Math.sin(mid) > 0 && Math.abs(Math.cos(mid)) * rin < rad) continue;
            ringPt(0, cx, cy, cz, a0, rin, flat, ct, st, 0xE6D2FF, a * 0.75f);
            ringPt(1, cx, cy, cz, a0, rout, flat, ct, st, 0xB898FF, a * 0.25f);
            ringPt(2, cx, cy, cz, a1, rout, flat, ct, st, 0xB898FF, a * 0.25f);
            ringPt(3, cx, cy, cz, a1, rin, flat, ct, st, 0xE6D2FF, a * 0.75f);
            emit();
        }
    }

    private static void ringPt(int i, float cx, float cy, float cz, float ang, float rad, float flat, float ct, float st, int rgb, float a) {
        float px = (float) Math.cos(ang) * rad, py = (float) Math.sin(ang) * rad * flat;
        float qx = px * ct - py * st, qy = px * st + py * ct;
        set(i, cx + rX * qx + uX * qy, cy + rY * qx + uY * qy, cz + rZ * qx + uZ * qy, rgb, a);
    }

    // ---------------------------------------------------------------- солнце и радуга

    private static void sunGlow(float th, float clear, float r, long now) {
        float dx = -(float) Math.sin(th), dy = (float) Math.cos(th);
        if (dy < -0.05f) return;
        float vis = clear * smooth((dy + 0.05f) / 0.12f);
        if (vis < 0.02f) return;
        boolean strong = Modules.SUN_GLOW.index == 2;
        float k = strong ? 1f : 0.6f;
        float warm = 1f - smooth(dy / 0.5f);                              // ниже солнце = теплее
        int core = mix(0xFFF4D0, 0xFFB060, warm), halo = mix(0xFFE2A0, 0xFF7A3A, warm);
        basis(dx, dy, 0f);
        float cx = dx * r, cy = dy * r, cz = 0f;
        fan(cx, cy, cz, r * tanDeg(strong ? 24f : 15f), halo, 0.16f * k * vis, halo, 0f, 28);
        fan(cx, cy, cz, r * tanDeg(9f), core, 0.30f * k * vis, core, 0f, 20);
        int n = strong ? 18 : 10;
        if (Perf.reduced()) n /= 2;
        float rot = (float) (now / 1000.0 * 0.015);                       // очень медленное вращение лучей
        for (int i = 0; i < n; i++) {
            float ang = i * TAU / n + rot + 0.3f * hash(i, 5);
            float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
            float len = r * tanDeg((strong ? 34f : 22f) * (0.6f + 0.4f * frac(i * 0.618f)));
            float wd = r * tanDeg(strong ? 1.8f : 1.2f);
            float vx = rX * ca + uX * sa, vy = rY * ca + uY * sa, vz = rZ * ca + uZ * sa;
            float sx = -(rX * sa) + uX * ca, sy = -(rY * sa) + uY * ca, sz = -(rZ * sa) + uZ * ca;
            float a0 = 0.22f * k * vis;
            set(0, cx + sx * wd, cy + sy * wd, cz + sz * wd, core, a0);
            set(1, cx + vx * len, cy + vy * len, cz + vz * len, halo, 0f);
            set(2, cx + vx * len, cy + vy * len, cz + vz * len, halo, 0f);
            set(3, cx - sx * wd, cy - sy * wd, cz - sz * wd, core, a0);
            emit();
        }
    }

    private static final int[] RB = { 0xFF3B30, 0xFF9A1F, 0xFFE21F, 0x3BE05A, 0x2EC8FF, 0x3A7BFF, 0x8A4BFF };

    /** радуга: дуга 42 градуса вокруг точки напротив солнца, видна при низком солнце */
    private static void rainbow(float th, float clear, float r) {
        float sunY = (float) Math.cos(th);
        float f = smooth((sunY - 0.04f) / 0.12f) * (1f - smooth((sunY - 0.55f) / 0.20f));
        f *= 0.35f + 0.65f * clear;
        if (f < 0.02f) return;
        float ax = (float) Math.sin(th), ay = -(float) Math.cos(th);      // противосолнечная точка
        float e2x = ay, e2y = -ax;                                        // вторая ось дуги (первая = Z)
        float amp = Modules.RAINBOW.index == 2 ? 0.26f : 0.14f;
        int steps = Perf.reduced() ? 48 : 90;
        float outer = 43.5f, band = 0.64f;
        for (int b = 0; b < RB.length; b++) {
            float rad0 = (float) Math.toRadians(outer - band * (b + 1)), rad1 = (float) Math.toRadians(outer - band * b);
            for (int s = 0; s < steps; s++) {
                float p0 = s * TAU / steps, p1 = (s + 1) * TAU / steps;
                float c0 = (float) Math.cos(p0), s0 = (float) Math.sin(p0), c1 = (float) Math.cos(p1), s1 = (float) Math.sin(p1);
                float y00 = arcY(ay, e2y, rad0, s0), y11 = arcY(ay, e2y, rad1, s1);
                if (y00 < 0.0f && y11 < 0.0f) continue;
                rbPt(0, ax, ay, e2x, e2y, rad0, c0, s0, r, RB[b], amp * f);
                rbPt(1, ax, ay, e2x, e2y, rad1, c0, s0, r, RB[b], amp * f);
                rbPt(2, ax, ay, e2x, e2y, rad1, c1, s1, r, RB[b], amp * f);
                rbPt(3, ax, ay, e2x, e2y, rad0, c1, s1, r, RB[b], amp * f);
                emit();
            }
        }
    }

    /** y-компонента точки дуги (для отсечения нижней половины) */
    private static float arcY(float ay, float e2y, float rad, float s) {
        return ay * (float) Math.cos(rad) + (float) Math.sin(rad) * (e2y * s);
    }

    private static void rbPt(int i, float ax, float ay, float e2x, float e2y, float rad, float c, float s, float r, int rgb, float a) {
        float cr = (float) Math.cos(rad), sr = (float) Math.sin(rad);
        float x = ax * cr + sr * (e2x * s);
        float y = ay * cr + sr * (e2y * s);
        float z = sr * c;                                                 // первая ось дуги = Z
        float fa = a * smooth(y / 0.10f);                                 // у горизонта дуга плавно гаснет
        set(i, x * r, y * r, z * r, rgb, fa);
    }

    // ---------------------------------------------------------------- подсветка блока под прицелом

    private static final int[][] Q = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };
    private static final float[] SB = { -1f, 1f, 1f, -1f }, SC = { -1f, -1f, 1f, 1f };

    private static int cornerRgb(int ix, int iy, int iz, long now) {
        float u = 0.5f + 0.5f * (float) Math.sin(now / 1500.0 + ix * 1.0 + iy * 2.0 + iz * 3.0);
        return FxColor.pick(Modules.BLOCK_COLOR, Modules.BLOCK_COLOR2, u, now) & 0xFFFFFF;
    }

    private static void blockGlow(WorldRenderContext ctx, MinecraftClient mc, ClientWorld w, long now) {
        HitResult h = mc.crosshairTarget;
        if (!(h instanceof BlockHitResult bh) || h.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bh.getBlockPos();
        BlockState st = w.getBlockState(pos);
        VoxelShape sh = st.getOutlineShape(w, pos);
        if (sh.isEmpty()) return;
        Box b = sh.getBoundingBox();
        Vec3d cam = ctx.camera().getPos();
        float e = 0.004f;
        float[] lo = { (float) (pos.getX() + b.minX - cam.x) - e, (float) (pos.getY() + b.minY - cam.y) - e, (float) (pos.getZ() + b.minZ - cam.z) - e };
        float[] hi = { (float) (pos.getX() + b.maxX - cam.x) + e, (float) (pos.getY() + b.maxY - cam.y) + e, (float) (pos.getZ() + b.maxZ - cam.z) + e };
        int mode = Modules.BLOCK_GLOW.index;                              // 1 = Outline, 2 = Fill, 3 = Both
        float pulse = Modules.COMFORT.enabled ? 1f : 0.85f + 0.15f * (float) Math.sin(now / 500.0);
        int[] idx = new int[3];
        if (mode == 2 || mode == 3) {
            for (int a = 0; a < 3; a++) {
                for (int s = 0; s < 2; s++) {
                    int bi = (a + 1) % 3, ci = (a + 2) % 3;
                    for (int k = 0; k < 4; k++) {
                        idx[a] = s;
                        idx[bi] = Q[k][0];
                        idx[ci] = Q[k][1];
                        set(k, idx[0] == 0 ? lo[0] : hi[0], idx[1] == 0 ? lo[1] : hi[1], idx[2] == 0 ? lo[2] : hi[2],
                                cornerRgb(idx[0], idx[1], idx[2], now), 0.16f * pulse);
                    }
                    emit();
                }
            }
        }
        if (mode == 1 || mode == 3) {
            float t = 0.012f;
            for (int a = 0; a < 3; a++) {
                int bi = (a + 1) % 3, ci = (a + 2) % 3;
                for (int bc = 0; bc < 4; bc++) {
                    int ib = bc & 1, ic = bc >> 1;
                    float[] p0 = new float[3], p1 = new float[3];
                    idx[a] = 0;
                    idx[bi] = ib;
                    idx[ci] = ic;
                    for (int q = 0; q < 3; q++) p0[q] = idx[q] == 0 ? lo[q] : hi[q];
                    int c0 = cornerRgb(idx[0], idx[1], idx[2], now);
                    idx[a] = 1;
                    for (int q = 0; q < 3; q++) p1[q] = idx[q] == 0 ? lo[q] : hi[q];
                    int c1 = cornerRgb(idx[0], idx[1], idx[2], now);
                    for (int k = 0; k < 4; k++) {
                        int kk = (k + 1) & 3;
                        float[] a0 = p0.clone(), a1 = p1.clone(), a2 = p1.clone(), a3 = p0.clone();
                        a0[bi] += SB[k] * t;  a0[ci] += SC[k] * t;
                        a1[bi] += SB[k] * t;  a1[ci] += SC[k] * t;
                        a2[bi] += SB[kk] * t; a2[ci] += SC[kk] * t;
                        a3[bi] += SB[kk] * t; a3[ci] += SC[kk] * t;
                        set(0, a0[0], a0[1], a0[2], c0, 0.85f * pulse);
                        set(1, a1[0], a1[1], a1[2], c1, 0.85f * pulse);
                        set(2, a2[0], a2[1], a2[2], c1, 0.85f * pulse);
                        set(3, a3[0], a3[1], a3[2], c0, 0.85f * pulse);
                        emit();
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- частицы воздуха (раз в тик)

    private static final float[] AIR_BASE = { 0f, 0.7f, 0.5f, 3.0f, 0.8f, 1.5f, 0.8f };

    public static void tick(MinecraftClient mc) {
        int mode = Modules.AIR_FX.index;
        ClientWorld w = mc.world;
        if (mode <= 0 || mode >= AIR_BASE.length || w == null || mc.player == null || mc.isPaused()) return;
        if (Modules.PARTICLE_MODE.index == 2 || mc.player.isSubmergedInWater()) return;
        float mult = new float[] { 1f, 0.5f, 1.8f }[Modules.AIR_AMOUNT.index];
        if (Perf.reduced()) mult *= 0.5f;
        float night = smooth((float) (0.5 - Math.cos(w.getSkyAngle(1f) * Math.PI * 2.0) * 0.9));
        if (mode == 1 && (night < 0.25f || w.getRainGradient(1f) > 0.5f)) return;   // светлячки только ясной ночью
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float want = AIR_BASE[mode] * mult;
        int count = (int) want + (rnd.nextFloat() < want - (int) want ? 1 : 0);
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0, rad = 3.0 + rnd.nextDouble() * 11.0;
            double x = px + Math.cos(ang) * rad, z = pz + Math.sin(ang) * rad, y;
            switch (mode) {
                case 1 -> y = py + 0.3 + rnd.nextDouble() * 3.0;
                case 4 -> y = py - 0.5 + rnd.nextDouble() * 2.5;
                case 6 -> y = py + 0.5 + rnd.nextDouble() * 4.0;
                default -> y = py + 2.0 + rnd.nextDouble() * 7.0;
            }
            BlockPos bp = BlockPos.ofFloored(x, y, z);
            if (!w.getBlockState(bp).isAir()) continue;
            if ((mode == 2 || mode == 3 || mode == 5) && !w.isSkyVisible(bp)) continue;   // под крышей лепестки и снег не падают
            if (NeonComfort.nearCam(x, y, z, 1.5)) continue;
            switch (mode) {
                case 1 -> {                                                              // светлячки
                    Particle p = mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z,
                            (rnd.nextDouble() - 0.5) * 0.01, (rnd.nextDouble() - 0.5) * 0.006, (rnd.nextDouble() - 0.5) * 0.01);
                    if (p != null) {
                        p.setColor(0.75f + rnd.nextFloat() * 0.15f, 1f, 0.30f + rnd.nextFloat() * 0.2f);
                        p.scale(0.45f);
                    }
                }
                case 2 -> add(mc, ParticleTypes.CHERRY_LEAVES, x, y, z, 0.02, -0.02, 0.0);
                case 3 -> add(mc, ParticleTypes.SNOWFLAKE, x, y, z, (rnd.nextDouble() - 0.5) * 0.02, -0.05, (rnd.nextDouble() - 0.5) * 0.02);
                case 4 -> {                                                              // искры
                    Particle p = mc.particleManager.addParticle(ParticleTypes.SMALL_FLAME, x, y, z,
                            (rnd.nextDouble() - 0.5) * 0.01, 0.03, (rnd.nextDouble() - 0.5) * 0.01);
                    if (p != null) p.scale(0.6f);
                }
                case 5 -> add(mc, ParticleTypes.WHITE_ASH, x, y, z, (rnd.nextDouble() - 0.5) * 0.01, -0.01, (rnd.nextDouble() - 0.5) * 0.01);
                case 6 -> add(mc, ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0.0, -0.005, 0.0);
                default -> { }
            }
        }
    }

    private static void add(MinecraftClient mc, ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
        mc.particleManager.addParticle(e, x, y, z, vx, vy, vz);
    }

    private WorldVisuals() {}
}
