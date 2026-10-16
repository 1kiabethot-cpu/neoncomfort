package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Питомцы: панда, жаба, нюхач и пчела ходят (или летают) рядом с вами.
 * Это чисто клиентские существа: сервер и другие игроки их не видят, ударить или выбрать прицелом их нельзя.
 */
public final class Pets {
    private static LivingEntity pet;
    private static int kind = 0;
    private static double tx, ty, tz;          // целевая точка, к которой идёт питомец
    private static float refYaw;               // сглаженное направление «вперёд» (чтобы питомец не кружил при поворотах камеры)
    private static boolean refInit = false;
    private static double groundY = 0.0;
    private static float appliedScale = -1f;
    private static final float[] BASE_SCALE = { 1.0f, 0.45f, 1.2f, 0.35f, 1.3f };   // индекс = вид питомца
    private static final float[] SIZE_MULT = { 0.7f, 1.0f, 1.4f };                  // Small / Normal / Big

    /** Вызывается каждый клиентский тик. */
    public static void tick(MinecraftClient mc, ClientPlayerEntity p) {
        int want = Modules.PET.index;
        ClientWorld w = mc.world;
        if (want == 0 || w == null) {
            remove(mc);
            return;
        }
        if (pet != null && (kind != want || pet.isRemoved() || pet.getWorld() != w)) remove(mc);
        if (pet == null) {
            pet = create(want, w);
            if (pet == null) return;
            kind = want;
            pet.setId(0x3F000000 + want);          // большой номер, чтобы не совпасть с настоящими существами сервера
            pet.noClip = true;
            pet.refreshPositionAndAngles(p.getX() - 1.4, p.getY(), p.getZ() - 1.4, p.getYaw(), 0f);
            w.addEntity(pet);
            appliedScale = -1f;
            refInit = false;
        }

        // направление «вперёд» меняем плавно
        if (!refInit) {
            refYaw = p.getYaw();
            refInit = true;
        }
        refYaw += MathHelper.wrapDegrees(p.getYaw() - refYaw) * 0.06f;
        double r = Math.toRadians(refYaw);
        double fx = -Math.sin(r), fz = Math.cos(r);       // вперёд
        double rx = -Math.cos(r), rz = -Math.sin(r);      // вправо
        tx = p.getX() - fx * 1.9 + rx * 1.1;              // сзади-справа от игрока
        tz = p.getZ() - fz * 1.9 + rz * 1.1;
        if (p.isOnGround() || Math.abs(p.getY() - groundY) > 3.0) groundY = p.getY();
        ty = kind == 4 ? p.getY() + 1.6 + Math.sin(p.age * 0.1) * 0.15 : groundY;

        // размер питомца
        int si = Math.max(0, Math.min(SIZE_MULT.length - 1, Modules.PET_SIZE.index));
        float scale = BASE_SCALE[kind] * SIZE_MULT[si];
        if (Math.abs(scale - appliedScale) > 0.001f) {
            EntityAttributeInstance a = pet.getAttributeInstance(EntityAttributes.SCALE);
            if (a != null) a.setBaseValue(scale);
            appliedScale = scale;
        }
    }

    /** Шаг движения питомца к цели; вызывается из его собственного tick(), до расчёта анимации ног. */
    static void step(LivingEntity e) {
        if (e != pet) return;
        boolean fly = kind == 4;
        double dx = tx - e.getX(), dz = tz - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 18.0 || Math.abs(ty - e.getY()) > 8.0) {          // слишком далеко: телепорт к игроку
            e.refreshPositionAndAngles(tx, ty, tz, e.getYaw(), 0f);
            return;
        }
        double nx = e.getX(), nz = e.getZ();
        if (d > 0.4) {
            double sp = Math.min(d * 0.22, fly ? 0.36 : 0.30);
            nx += dx / d * sp;
            nz += dz / d * sp;
            float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float cur = e.getYaw();
            float ny = cur + MathHelper.wrapDegrees(want - cur) * 0.35f;
            e.setYaw(ny);
            e.setBodyYaw(ny);
            e.setHeadYaw(ny);
        }
        double yy = e.getY() + (ty - e.getY()) * (fly ? 0.12 : 0.35);
        e.setPosition(nx, yy, nz);
        e.setOnGround(!fly);
    }

    /** Если что-то пошло не так, питомца выключаем, а не роняем игру. */
    static void fail() {
        Modules.PET.index = 0;
        pet = null;
    }

    static void remove(MinecraftClient mc) {
        if (pet == null) return;
        try {
            if (mc.world != null) mc.world.removeEntity(pet.getId(), Entity.RemovalReason.DISCARDED);
            else pet.discard();
        } catch (RuntimeException ignored) {
        }
        pet = null;
    }

    private static LivingEntity create(int k, ClientWorld w) {
        switch (k) {
            case 1:
                return new PandaEntity(EntityType.PANDA, w) {
                    @Override
                    public void tick() {
                        try {
                            Pets.step(this);
                            super.tick();
                        } catch (RuntimeException ex) {
                            Pets.fail();
                        }
                    }

                    public boolean canHit() { return false; }
                };
            case 2:
                return new FrogEntity(EntityType.FROG, w) {
                    @Override
                    public void tick() {
                        try {
                            Pets.step(this);
                            super.tick();
                        } catch (RuntimeException ex) {
                            Pets.fail();
                        }
                    }

                    public boolean canHit() { return false; }
                };
            case 3:
                return new SnifferEntity(EntityType.SNIFFER, w) {
                    @Override
                    public void tick() {
                        try {
                            Pets.step(this);
                            super.tick();
                        } catch (RuntimeException ex) {
                            Pets.fail();
                        }
                    }

                    public boolean canHit() { return false; }
                };
            case 4:
                return new BeeEntity(EntityType.BEE, w) {
                    @Override
                    public void tick() {
                        try {
                            Pets.step(this);
                            super.tick();
                        } catch (RuntimeException ex) {
                            Pets.fail();
                        }
                    }

                    public boolean canHit() { return false; }
                };
            default:
                return null;
        }
    }

    private Pets() {}
}
