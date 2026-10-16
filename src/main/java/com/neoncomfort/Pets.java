package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Питомцы: панда, жаба, нюхач, пчела, оцелот, кот, лиса, волк, кролик, попугай, аксолотль и алей.
 * Они стоят рядом с вами, идут (или летят) за вами и иногда делают свои обычные игровые анимации
 * (садятся, квакают, нюхают). Это чисто клиентские существа: сервер и другие игроки их не видят,
 * ударить их или прицелиться в них нельзя.
 */
public final class Pets {
    private static LivingEntity pet;
    private static int kind = 0;
    private static double tx, ty, tz;           // точка, к которой идёт питомец
    private static float refYaw;                // сглаженное направление «вперёд» (чтобы питомец не кружил при поворотах камеры)
    private static boolean refInit = false;
    private static double groundY = 0.0;
    private static float appliedScale = -1f;
    private static boolean moving = false;      // выставляется в step()
    private static boolean acting = false;      // сейчас играет «действие» (сидит, квакает, нюхает)
    private static int idleTicks = 0;
    private static int actionTicks = 0;

    //                                         0     1      2     3      4     5     6     7     8     9     10    11    12
    private static final float[] BASE_SCALE = { 1f, 0.45f, 1.2f, 0.35f, 1.3f, 1.0f, 1.0f, 1.0f, 0.9f, 1.3f, 1.3f, 1.3f, 1.0f };
    private static final float[] SIZE_MULT = { 0.7f, 1.0f, 1.4f };                  // Small / Normal / Big
    private static final double[] FLY_HEIGHT = { 0, 0, 0, 0, 1.6, 0, 0, 0, 0, 0, 1.5, 0, 1.4 };

    private static boolean flies(int k) { return k == 4 || k == 10 || k == 12; }

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
            try {
                pet = create(want, w);
                if (pet == null) return;
                kind = want;
                pet.setId(0x3F000000 + want);          // большой номер, чтобы не совпасть с настоящими существами сервера
                pet.noClip = true;
                pet.refreshPositionAndAngles(p.getX() - 1.4, p.getY(), p.getZ() - 1.4, p.getYaw(), 0f);
                w.addEntity(pet);
            } catch (RuntimeException ex) {
                fail();
                return;
            }
            appliedScale = -1f;
            refInit = false;
            acting = false;
            idleTicks = 0;
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
        boolean fly = flies(kind);
        double side = fly ? 0.95 : 1.05;                  // питомец стоит рядом, справа
        double back = fly ? 0.2 : 0.25;
        tx = p.getX() - fx * back + rx * side;
        tz = p.getZ() - fz * back + rz * side;
        if (p.isOnGround() || Math.abs(p.getY() - groundY) > 3.0) groundY = p.getY();
        ty = fly ? p.getY() + FLY_HEIGHT[kind] + Math.sin(p.age * 0.1) * 0.15 : groundY;

        // размер питомца
        int si = Math.max(0, Math.min(SIZE_MULT.length - 1, Modules.PET_SIZE.index));
        float scale = BASE_SCALE[kind] * SIZE_MULT[si];
        if (Math.abs(scale - appliedScale) > 0.001f) {
            EntityAttributeInstance a = pet.getAttributeInstance(EntityAttributes.SCALE);
            if (a != null) a.setBaseValue(scale);
            appliedScale = scale;
        }

        animate(pet);
    }

    /** Если питомец стоит, он время от времени делает свою игровую анимацию; при движении сразу прекращает. */
    private static void animate(LivingEntity e) {
        if (flies(kind)) return;
        if (moving) {
            idleTicks = 0;
            if (acting) endAction(e);
            return;
        }
        idleTicks++;
        if (acting) {
            if (--actionTicks <= 0) endAction(e);
            return;
        }
        if (idleTicks > 70 && Math.random() < 0.015) startAction(e);
    }

    private static void startAction(LivingEntity e) {
        switch (kind) {
            case 1:
                ((PandaEntity) e).setSitting(true);
                actionTicks = 90 + (int) (Math.random() * 60);
                break;
            case 2:
                e.setPose(EntityPose.CROAKING);
                actionTicks = 45;
                break;
            case 3:
                ((SnifferEntity) e).startState(Math.random() < 0.5 ? SnifferEntity.State.FEELING_HAPPY : SnifferEntity.State.SNIFFING);
                actionTicks = 80;
                break;
            case 6:
                ((CatEntity) e).setInSittingPose(true);
                actionTicks = 140;
                break;
            case 7:
                ((FoxEntity) e).setSitting(true);
                actionTicks = 120;
                break;
            case 8:
                ((WolfEntity) e).setInSittingPose(true);
                actionTicks = 140;
                break;
            default:
                return;
        }
        acting = true;
    }

    private static void endAction(LivingEntity e) {
        switch (kind) {
            case 1: ((PandaEntity) e).setSitting(false); break;
            case 2: e.setPose(EntityPose.STANDING); break;
            case 3: ((SnifferEntity) e).startState(SnifferEntity.State.IDLING); break;
            case 6: ((CatEntity) e).setInSittingPose(false); break;
            case 7: ((FoxEntity) e).setSitting(false); break;
            case 8: ((WolfEntity) e).setInSittingPose(false); break;
            default: break;
        }
        acting = false;
        idleTicks = 0;
    }

    /** Обёртка над tick() питомца: сначала шаг к цели (чтобы ноги анимировались), затем обычный tick. */
    static void run(LivingEntity e, Runnable superTick) {
        try {
            step(e);
            superTick.run();
        } catch (RuntimeException ex) {
            fail();
        }
    }

    /** Шаг движения питомца к цели; вызывается из его собственного tick(), до расчёта анимации ног. */
    private static void step(LivingEntity e) {
        if (e != pet) return;
        boolean fly = flies(kind);
        double dx = tx - e.getX(), dz = tz - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 18.0 || Math.abs(ty - e.getY()) > 8.0) {          // слишком далеко: телепорт к игроку
            e.refreshPositionAndAngles(tx, ty, tz, e.getYaw(), 0f);
            moving = false;
            return;
        }
        moving = d > 0.4;
        double nx = e.getX(), nz = e.getZ();
        if (moving) {
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
        acting = false;
    }

    private static LivingEntity create(int k, ClientWorld w) {
        switch (k) {
            case 1:
                return new PandaEntity(EntityType.PANDA, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 2:
                return new FrogEntity(EntityType.FROG, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 3:
                return new SnifferEntity(EntityType.SNIFFER, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 4:
                return new BeeEntity(EntityType.BEE, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 5:
                return new OcelotEntity(EntityType.OCELOT, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 6:
                return new CatEntity(EntityType.CAT, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 7:
                return new FoxEntity(EntityType.FOX, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 8:
                return new WolfEntity(EntityType.WOLF, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 9:
                return new RabbitEntity(EntityType.RABBIT, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 10:
                return new ParrotEntity(EntityType.PARROT, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 11:
                return new AxolotlEntity(EntityType.AXOLOTL, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            case 12:
                return new AllayEntity(EntityType.ALLAY, w) {
                    @Override
                    public void tick() { Pets.run(this, () -> super.tick()); }

                    public boolean canHit() { return false; }
                };
            default:
                return null;
        }
    }

    private Pets() {}
}
