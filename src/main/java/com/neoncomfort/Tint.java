package com.neoncomfort;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;

/** Tints non-dust particles (heart, totem, ...) so Hit/Trail colors work for every effect. */
public final class Tint {
    private static int lastId;
    private static int lastColor = 0xFFFFFF;
    private static boolean inited;
    private static boolean failed;
    private static Field pmField;
    private static Method addM;
    private static Method colM;

    private Tint() {}

    /** false после первой неудачи рефлексии: тогда используется запасной вариант */
    public static boolean ok() {
        return !failed;
    }

    public static ParticleEffect effect(int id, int color, float scale) {
        lastId = id;
        lastColor = color;
        return NeonComfort.effect(id, color, scale);
    }

    public static void spawn(ClientWorld w, ParticleEffect fx, double x, double y, double z,
                             double vx, double vy, double vz) {
        if (lastId != 0 && !failed) {
            try {
                if (!inited) init();
                Object pm = pmField.get(MinecraftClient.getInstance());
                Object p = addM.invoke(pm, fx, x, y, z, vx, vy, vz);
                if (p != null) {
                    int c = lastColor;
                    colM.invoke(p, ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
                }
                return;
            } catch (Throwable t) {
                failed = true;
            }
        }
        w.addParticle(fx, x, y, z, vx, vy, vz);
    }

    private static void init() throws Exception {
        Class<?> partC = null;
        outer:
        for (Field f : MinecraftClient.class.getDeclaredFields()) {
            try {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t.isArray() || t == String.class) continue;
                for (Method m : t.getMethods()) {
                    Class<?>[] ps = m.getParameterTypes();
                    Class<?> r = m.getReturnType();
                    if (r == void.class || r.isPrimitive() || ps.length != 7 || ps[0] != ParticleEffect.class) continue;
                    boolean ok = true;
                    for (int i = 1; i < 7; i++) if (ps[i] != double.class) ok = false;
                    if (!ok) continue;
                    f.setAccessible(true);
                    pmField = f;
                    addM = m;
                    partC = r;
                    break outer;
                }
            } catch (Throwable ignored) {
            }
        }
        if (partC == null) throw new IllegalStateException("addParticle not found");
        for (Method m : partC.getMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() == void.class && ps.length == 3
                    && ps[0] == float.class && ps[1] == float.class && ps[2] == float.class) {
                colM = m;
                break;
            }
        }
        if (colM == null) throw new IllegalStateException("setColor not found");
        inited = true;
    }
}
