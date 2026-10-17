package com.neoncomfort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class Cosmetics implements ClientModInitializer {
    static final Identifier CAPE_TEX = Identifier.of("neoncomfort", "textures/entity/cape.png");
    private static float capeLean = 0f;
    private static long capeLastNs = 0L;
    static final Identifier TEX = Identifier.of("neoncomfort", "textures/entity/cosmetics.png");
    static final int WING_STYLES = 5;
    static final int HAT_STYLES = 11;
    static final int[] WING_V = {0, 21, 42, 63, 84};
    static final int MAX_CUBES = 10;
    static final int RING_SEGS = 18;
    static final int MAX_RINGS = 3;
    static final int FULL_LIGHT = 0xF000F0;

    static final ModelPart WINGS = buildWings();
    static final ModelPart HATS = buildHats();
    static final ModelPart CUBES = buildCubes();
    static final ModelPart RINGS = buildRings();
    static final int SLASH_SEGS = 16;
    static final ModelPart SLASH = buildSlash();
    static final int TAIL_SEGS = 14;
    private static float orbitAngle = 0f;    // текущий угол вращения клинков
    private static long orbitLastNs = 0L;
    private static float tailLean = 0f;     // сглаженный наклон хвоста назад
    private static long tailLastNs = 0L;

    @Override
    public void onInitializeClient() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, entityRenderer, registrationHelper, context) -> {
                if (entityRenderer instanceof PlayerEntityRenderer) {
                    registrationHelper.register(new CosmeticsFeature((PlayerEntityRenderer) entityRenderer));
                }
            });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && Modules.HIT_RING.index > 0 && entity != null) {
                spawnHitRing(entity, Modules.HIT_RING.index);
            }
            return ActionResult.PASS;
        });
    }

    // ---------------------------------------------------------------- models

    private static ModelPart buildWings() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int i = 0; i < WING_STYLES; i++) {
            int v = WING_V[i];
            root.addChild("wl" + i, ModelPartBuilder.create().uv(0, v).cuboid(0f, 0f, 0f, 24f, 20f, 1f),
                ModelTransform.pivot(1.0f, -8.0f, 2.4f));
            root.addChild("wr" + i, ModelPartBuilder.create().mirrored().uv(0, v).cuboid(-24f, 0f, 0f, 24f, 20f, 1f),
                ModelTransform.pivot(-1.0f, -8.0f, 2.4f));
        }
        return TexturedModelData.of(data, 128, 256).createModel();
    }

    private static ModelPart buildCubes() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int i = 0; i < MAX_CUBES; i++) {
            root.addChild("c" + i, ModelPartBuilder.create().uv(0, 108).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
                ModelTransform.NONE);
        }
        return TexturedModelData.of(data, 128, 256).createModel();
    }

    private static ModelPart buildRings() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int r = 0; r < MAX_RINGS; r++) {
            for (int i = 0; i < RING_SEGS; i++) {
                root.addChild("r" + r + "_" + i,
                    ModelPartBuilder.create().uv(0, 108).cuboid(-0.5f, -0.5f, -2f, 1f, 1f, 4f),
                    ModelTransform.NONE);
            }
        }
        return TexturedModelData.of(data, 128, 256).createModel();
    }

    /** Сегменты полумесяца для Slash FX: тонкие плоские пластины, берут пиксели из белой области (0,113). */
    private static ModelPart buildSlash() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int i = 0; i < SLASH_SEGS; i++) {
            root.addChild("s" + i, ModelPartBuilder.create().uv(0, 113).cuboid(-2.5f, -3f, -0.5f, 5f, 6f, 1f),
                ModelTransform.NONE);
        }
        return TexturedModelData.of(data, 128, 256).createModel();
    }

    private static ModelPart buildHats() {
        ModelData data = new ModelData();
        ModelPartData hats = data.getRoot();
        ModelPartData h;
        h = hats.addChild("hat1", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56,0).cuboid(-5f,-10f,-5f,10f,2f,1f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(78,0).cuboid(-5f,-10f,4f,10f,2f,1f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(100,0).cuboid(-5f,-10f,-4f,1f,2f,8f), ModelTransform.NONE);
        h.addChild("p3", ModelPartBuilder.create().uv(56,10).cuboid(4f,-10f,-4f,1f,2f,8f), ModelTransform.NONE);
        h.addChild("p4", ModelPartBuilder.create().uv(74,10).cuboid(-5f,-13f,-5f,2f,3f,2f), ModelTransform.NONE);
        h.addChild("p5", ModelPartBuilder.create().uv(82,10).cuboid(3f,-13f,-5f,2f,3f,2f), ModelTransform.NONE);
        h.addChild("p6", ModelPartBuilder.create().uv(90,10).cuboid(-5f,-13f,3f,2f,3f,2f), ModelTransform.NONE);
        h.addChild("p7", ModelPartBuilder.create().uv(98,10).cuboid(3f,-13f,3f,2f,3f,2f), ModelTransform.NONE);
        h.addChild("p8", ModelPartBuilder.create().uv(106,10).cuboid(-1f,-14f,-5f,2f,4f,1f), ModelTransform.NONE);
        h.addChild("p9", ModelPartBuilder.create().uv(112,10).cuboid(-1f,-14f,4f,2f,4f,1f), ModelTransform.NONE);
        h.addChild("p10", ModelPartBuilder.create().uv(118,10).cuboid(-1f,-9f,-6f,2f,1f,1f), ModelTransform.NONE);
        h = hats.addChild("hat2", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56,20).cuboid(-5f,-14f,-5f,10f,1f,1f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(78,20).cuboid(-5f,-14f,4f,10f,1f,1f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(100,20).cuboid(-5f,-14f,-4f,1f,1f,8f), ModelTransform.NONE);
        h.addChild("p3", ModelPartBuilder.create().uv(56,29).cuboid(4f,-14f,-4f,1f,1f,8f), ModelTransform.NONE);
        h = hats.addChild("hat3", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56,38).cuboid(-7f,-9f,-7f,14f,1f,14f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(56,53).cuboid(-4f,-19f,-4f,8f,10f,8f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(88,53).cuboid(-5f,-12f,-5f,10f,3f,10f), ModelTransform.NONE);
        h = hats.addChild("hat4", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56,71).cuboid(1f,-10f,-3f,3f,2f,2f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(66,71).cuboid(-4f,-10f,-3f,3f,2f,2f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(76,71).cuboid(2f,-12f,-3f,2f,2f,2f), ModelTransform.NONE);
        h.addChild("p3", ModelPartBuilder.create().uv(84,71).cuboid(-4f,-12f,-3f,2f,2f,2f), ModelTransform.NONE);
        h.addChild("p4", ModelPartBuilder.create().uv(92,71).cuboid(2f,-10f,-4f,1f,2f,1f), ModelTransform.NONE);
        h.addChild("p5", ModelPartBuilder.create().uv(96,71).cuboid(-3f,-10f,-4f,1f,2f,1f), ModelTransform.NONE);
        h = hats.addChild("hat5", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(100,71).cuboid(1f,-10f,-3f,3f,2f,3f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(112,71).cuboid(2f,-12f,-3f,2f,2f,3f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(122,71).cuboid(3f,-14f,-3f,1f,2f,2f), ModelTransform.NONE);
        h.addChild("p3", ModelPartBuilder.create().uv(56,76).cuboid(-4f,-10f,-3f,3f,2f,3f), ModelTransform.NONE);
        h.addChild("p4", ModelPartBuilder.create().uv(68,76).cuboid(-4f,-12f,-3f,2f,2f,3f), ModelTransform.NONE);
        h.addChild("p5", ModelPartBuilder.create().uv(78,76).cuboid(-4f,-14f,-3f,1f,2f,2f), ModelTransform.NONE);
        h = hats.addChild("hat6", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56,81).cuboid(-7f,-9f,-7f,14f,1f,14f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(56,96).cuboid(-4f,-12f,-4f,8f,3f,8f), ModelTransform.NONE);
        h.addChild("p2", ModelPartBuilder.create().uv(88,96).cuboid(-3f,-15f,-3f,6f,3f,6f), ModelTransform.NONE);
        h.addChild("p3", ModelPartBuilder.create().uv(112,96).cuboid(-2f,-18f,-2f,4f,3f,4f), ModelTransform.NONE);
        h.addChild("p4", ModelPartBuilder.create().uv(56,107).cuboid(-1f,-21f,-1f,2f,3f,2f), ModelTransform.NONE);
        h.addChild("p5", ModelPartBuilder.create().uv(64,107).cuboid(-5f,-10f,-5f,10f,1f,10f), ModelTransform.NONE);
        // китайская шляпа: плоский конус из тонких слоёв (hat7) и копия для мягкого свечения (hat7g);
        // все слои берут пиксели из белой области (0,113), цвет и прозрачность задаются при отрисовке
        ModelPartData core = hats.addChild("hat7", ModelPartBuilder.create(), ModelTransform.NONE);
        ModelPartData shell = hats.addChild("hat7g", ModelPartBuilder.create(), ModelTransform.NONE);
        for (int i = 0; i < 8; i++) {
            float w = 14f - i * 1.75f;
            float y = -8.55f - 0.55f * i;
            core.addChild("p" + i, ModelPartBuilder.create().uv(0, 113).cuboid(-w / 2f, y, -w / 2f, w, 0.55f, w),
                ModelTransform.NONE);
            shell.addChild("p" + i, ModelPartBuilder.create().uv(0, 113).cuboid(-w / 2f, y, -w / 2f, w, 0.55f, w),
                ModelTransform.NONE);
        }
        // --- новые шапки (версия 2.6): рога, заячьи уши, наушники, тыква ---
        h = hats.addChild("hat8", ModelPartBuilder.create(), ModelTransform.NONE);
        for (int sg = -1; sg <= 1; sg += 2) {
            int k = (sg + 1) / 2;
            h.addChild("a" + k, ModelPartBuilder.create().uv(0, 128).cuboid(sg * 2.5f - 0.5f, -14.5f, -0.5f, 1f, 7f, 1f), ModelTransform.NONE);
            h.addChild("b" + k, ModelPartBuilder.create().uv(0, 128).cuboid(sg > 0 ? 3f : -5f, -12.5f, -0.5f, 2f, 1f, 1f), ModelTransform.NONE);
            h.addChild("c" + k, ModelPartBuilder.create().uv(0, 128).cuboid(sg > 0 ? 3.5f : -5.5f, -10.5f, -0.5f, 2f, 1f, 1f), ModelTransform.NONE);
        }
        h = hats.addChild("hat9", ModelPartBuilder.create(), ModelTransform.NONE);
        for (int sg = -1; sg <= 1; sg += 2) {
            int k = (sg + 1) / 2;
            ModelTransform tr = ModelTransform.of(sg * 2.5f, -8f, 0f, 0f, 0f, sg * 0.22f);
            h.addChild("e" + k, ModelPartBuilder.create().uv(56, 128).cuboid(-1f, -9f, -0.5f, 2f, 9f, 1f), tr);
            h.addChild("i" + k, ModelPartBuilder.create().uv(0, 144).cuboid(-0.5f, -8f, -0.85f, 1f, 7f, 0.35f), tr);
        }
        h = hats.addChild("hat10", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("band", ModelPartBuilder.create().uv(56, 144).cuboid(-4.5f, -9.5f, -0.5f, 9f, 1f, 1f), ModelTransform.NONE);
        h.addChild("sl", ModelPartBuilder.create().uv(56, 144).cuboid(-5.2f, -9.5f, -0.5f, 1f, 6f, 1f), ModelTransform.NONE);
        h.addChild("sr", ModelPartBuilder.create().uv(56, 144).cuboid(4.2f, -9.5f, -0.5f, 1f, 6f, 1f), ModelTransform.NONE);
        h.addChild("cl", ModelPartBuilder.create().uv(0, 160).cuboid(-6.7f, -5.5f, -2f, 2f, 4f, 4f), ModelTransform.NONE);
        h.addChild("cr", ModelPartBuilder.create().uv(0, 160).cuboid(4.7f, -5.5f, -2f, 2f, 4f, 4f), ModelTransform.NONE);
        h = hats.addChild("hat11", ModelPartBuilder.create(), ModelTransform.NONE);
        h.addChild("p0", ModelPartBuilder.create().uv(56, 160).cuboid(-3.5f, -13.5f, -3.5f, 7f, 5.5f, 7f), ModelTransform.NONE);
        h.addChild("p1", ModelPartBuilder.create().uv(0, 176).cuboid(-0.5f, -15.5f, -0.5f, 1f, 2f, 1f), ModelTransform.NONE);
        return TexturedModelData.of(data, 128, 256).createModel();
    }

    // ---------------------------------------------------------------- colors

    /** Цвет эффекта: c1 — основной (при auraFirst значение 0 = цвет ауры), c2 — второй цвет для смешивания (OFF = без смешивания). */
    static int fxColor(Modules.Module c1, Modules.Module c2, boolean auraFirst, float u, long shiftMs) {
        int i = c1.index;
        int a;
        if (auraFirst && i <= 0) a = auraColor();
        else a = FxColor.base(c1, shiftMs);
        int j = c2.index;
        if (j <= 0) return a;
        int b = Theme.colorRgb(j - 1) & 0xFFFFFF;
        return FxColor.lerp(a, b, Math.max(0f, Math.min(1f, u)));
    }

    static int auraColor() {
        int idx = Modules.AURA_COLOR.index;
        if (idx <= 0) return Theme.accent() & 0xFFFFFF;
        if (idx == Theme.colorCount() + 1) return Theme.rainbowRgb() & 0xFFFFFF;
        return Theme.colorRgb(idx - 1) & 0xFFFFFF;
    }

    static float mulSize(Modules.Module m) {
        int i = m.index;
        return i == 1 ? 0.7f : (i == 2 ? 1.4f : 1.0f);
    }

    static float mulSpeed(Modules.Module m) {
        int i = m.index;
        return i == 1 ? 0.5f : (i == 2 ? 1.8f : 1.0f);
    }

    /** Aura Glow: Normal / Soft / Bright changes how opaque the glowing parts are. */
    static int alphaOf(int base) {
        int i = Modules.AURA_GLOW.index;
        float k = i == 1 ? 0.55f : (i == 2 ? 1.35f : 1.0f);
        int a = (int) (base * k);
        return a > 255 ? 255 : a;
    }

    // ---------------------------------------------------------------- cubes

    private static void setCubeScale(float s) {
        for (int i = 0; i < MAX_CUBES; i++) {
            ModelPart cube = CUBES.getChild("c" + i);
            cube.xScale = s;
            cube.yScale = s;
            cube.zScale = s;
        }
    }

    static void renderCubes(MatrixStack m, VertexConsumerProvider vcp, float age, int amount) {
        int n = amount == 1 ? 4 : (amount == 2 ? 6 : 10);
        int motion = Modules.CUBE_MOTION.index;   // 0 circle, 1 oval, 2 chaos, 3 spiral
        float sp = mulSpeed(Modules.AURA_SPEED);
        float rs = mulSize(Modules.AURA_SIZE) * (1f + CosmeticsFx.burstAmount());
        float cs = mulSize(Modules.CUBE_SIZE);
        float spin = age * 0.05f * sp;
        float t = age * 0.04f * sp;
        for (int i = 0; i < MAX_CUBES; i++) {
            ModelPart cube = CUBES.getChild("c" + i);
            cube.visible = i < n;
            if (i >= n) continue;
            float a = spin + i * (float) (Math.PI * 2.0 / n);
            float x;
            float y;
            float z;
            if (motion == 1) {
                x = (float) Math.cos(a) * 16f;
                z = (float) Math.sin(a) * 7f;
                y = 11f + (float) Math.sin(a) * 5f;
            } else if (motion == 2) {
                x = (float) Math.sin(t * 1.3f + i * 1.7f) * 13f + (float) Math.sin(t * 0.7f + i) * 3f;
                z = (float) Math.sin(t * 1.1f + i * 2.3f + 1f) * 13f;
                y = 11f + (float) Math.sin(t * 1.7f + i * 3.1f) * 12f;
                a = t * 3f + i;
            } else if (motion == 3) {
                float p = (spin * 0.35f + i / (float) n) % 1f;
                float a2 = spin * 2f + p * 12.5f;
                float rad = 9f + (float) Math.sin(p * Math.PI) * 4f;
                x = (float) Math.cos(a2) * rad;
                z = (float) Math.sin(a2) * rad;
                y = 26f - p * 36f;
            } else {
                x = (float) Math.cos(a) * 11f;
                z = (float) Math.sin(a) * 11f;
                y = 11f + (float) Math.sin(spin * 2f + i) * 8f;
            }
            cube.pivotX = x * rs;
            cube.pivotY = 11f + (y - 11f) * rs;
            cube.pivotZ = z * rs;
            cube.yaw = a * 2f;
            cube.pitch = a;
        }
        int look = Modules.CUBE_LOOK.index;       // 0 solid, 1 glass, 2 glow
        int rgb = fxColor(Modules.CUBES_COLOR, Modules.CUBES_COLOR2, true,
                0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 900.0), 0L);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEX));
        if (look == 2) {
            setCubeScale(2.2f * cs);
            CUBES.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (alphaOf(0x38) << 24) | rgb);
        }
        setCubeScale(cs);
        int alpha = alphaOf(look == 0 ? 0xE0 : (look == 1 ? 0x70 : 0xD0));
        CUBES.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (alpha << 24) | rgb);
    }

    // ---------------------------------------------------------------- rings

    static void renderRings(MatrixStack m, VertexConsumerProvider vcp, float age, int mode) {
        // mode: 1 = one moving ring, 2 = three moving rings, 3 = magic circle on the ground
        float spin = age * 0.06f * mulSpeed(Modules.AURA_SPEED);
        float rs = mulSize(Modules.AURA_SIZE);
        int count = mode == 2 ? 3 : (mode == 1 ? 1 : 2);
        for (int r = 0; r < MAX_RINGS; r++) {
            boolean on = r < count;
            float radius;
            float y;
            float rot;
            if (mode == 3) {
                radius = (r == 0 ? 15f : 9f) * rs;
                y = 23.6f;
                rot = r == 0 ? spin : -spin * 1.4f;
            } else {
                radius = 10f * rs;
                y = 11f + (float) Math.sin(spin * 1.6f + r * 2.094f) * 11f;
                rot = spin * (r % 2 == 0 ? 1f : -1f);
            }
            for (int i = 0; i < RING_SEGS; i++) {
                ModelPart seg = RINGS.getChild("r" + r + "_" + i);
                seg.visible = on;
                if (!on) continue;
                float a = rot + i * (float) (Math.PI * 2.0 / RING_SEGS);
                seg.pivotX = (float) Math.cos(a) * radius;
                seg.pivotY = y;
                seg.pivotZ = (float) Math.sin(a) * radius;
                seg.yaw = -a;
                seg.xScale = 1.4f;
                seg.yScale = 1.4f;
                seg.zScale = radius / 10f * 1.1f;
            }
        }
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEX));
        RINGS.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (alphaOf(0xC0) << 24) | auraColor());
    }

    // ---------------------------------------------------------------- hit ring

    static void spawnHitRing(Entity e, int mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        int rgb = auraColor();
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;
        double cx = e.getX();
        double cz = e.getZ();
        double h = e.getHeight();
        double y0 = e.getY();
        double size = mulSize(Modules.HIT_RING_SIZE);
        if (mode == 1) {
            hitRing(mc, cx, y0 + h * 0.5, cz, 0.13 * size, r, g, b);
        } else if (mode == 2) {
            hitRing(mc, cx, y0 + h * 0.25, cz, 0.13 * size, r, g, b);
            hitRing(mc, cx, y0 + h * 0.75, cz, 0.13 * size, r, g, b);
        } else {
            for (int i = 0; i < 28; i++) {
                double vx = Math.random() * 2.0 - 1.0;
                double vy = Math.random() * 2.0 - 1.0;
                double vz = Math.random() * 2.0 - 1.0;
                double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (len < 1.0E-3) continue;
                double sp = 0.12 * size / len;
                Particle p = mc.particleManager.addParticle(ParticleTypes.END_ROD,
                    cx, y0 + h * 0.5, cz, vx * sp, vy * sp, vz * sp);
                if (p != null) {
                    p.setColor(r, g, b);
                    p.scale(1.4f);
                }
            }
        }
    }

    private static void hitRing(MinecraftClient mc, double x, double y, double z, double speed,
                                float r, float g, float b) {
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            Particle p = mc.particleManager.addParticle(ParticleTypes.END_ROD,
                x + Math.cos(a) * 0.3, y, z + Math.sin(a) * 0.3,
                Math.cos(a) * speed, 0.0, Math.sin(a) * speed);
            if (p != null) {
                p.setColor(r, g, b);
                p.scale(1.4f);
            }
        }
    }

    // ---------------------------------------------------------------- slash

    /** Светящийся полумесяц-взмах вокруг тела. Рисуется только моделью игрока (в 3-м лице), поэтому в первом лице ничего не закрывает. */
    static void renderSlash(MatrixStack m, VertexConsumerProvider vcp) {
        int mode = Modules.SLASH.index;
        if (mode <= 0 || NeonComfort.slashStartMs == 0L) return;
        float p = (System.currentTimeMillis() - NeonComfort.slashStartMs) / (float) NeonComfort.SLASH_MS;
        if (p < 0f || p >= 1f) return;
        float sweep = (float) Math.toRadians(mode == 2 ? 105 : 80);
        float radius = mode == 2 ? 27f : 21f;
        float ease = 1f - (1f - p) * (1f - p);            // быстрый старт, мягкий конец
        float head = -sweep + 2f * sweep * ease;
        float trail = sweep * 1.15f;                      // длина шлейфа
        float fade = p < 0.65f ? 1f : (1f - p) / 0.35f;
        float dir = NeonComfort.slashFlip ? -1f : 1f;
        int rgb = NeonComfort.slashRgb(0L);
        if (Modules.SLASH_COLOR2.index > 0) {            // второй цвет: цвет плавно меняется по ходу взмаха
            rgb = FxColor.lerp(rgb, Theme.colorRgb(Modules.SLASH_COLOR2.index - 1) & 0xFFFFFF, p);
        }
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEyes(TEX));
        int arcs = mode == 3 ? 2 : 1;
        for (int arc = 0; arc < arcs; arc++) {
            SLASH.pivotY = 5f;
            SLASH.roll = (float) Math.toRadians(38.0) * dir * (arc == 0 ? 1f : -1f);
            for (int pass = 0; pass < 2; pass++) {        // 0 = мягкое свечение, 1 = ядро
                float grow = pass == 0 ? 1.8f : 1f;
                for (int i = 0; i < SLASH_SEGS; i++) {
                    ModelPart seg = SLASH.getChild("s" + i);
                    float u = i / (float) (SLASH_SEGS - 1);        // 0 = хвост, 1 = голова
                    float a = head - trail * (1f - u);
                    boolean on = a >= -sweep;
                    seg.visible = on;
                    if (!on) continue;
                    seg.pivotX = (float) Math.sin(a) * radius * dir;
                    seg.pivotZ = -(float) Math.cos(a) * radius;     // перед игроком = -Z в модели
                    seg.pivotY = 0f;
                    seg.yaw = -a * dir;
                    seg.xScale = 1f;
                    seg.yScale = (0.12f + 1.3f * (float) Math.sin(Math.PI * u)) * grow;
                    seg.zScale = pass == 0 ? 2.6f : 0.6f;
                }
                int alpha = (int) ((pass == 0 ? 0x38 : 0xB4) * fade);
                SLASH.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (alpha << 24) | rgb);
            }
        }
    }

    // ---------------------------------------------------------------- china hat / tail

    /** Прозрачная светящаяся китайская шляпа (цвет как у кубов/ауры). Матрица уже в системе головы. */
    static void renderChinaHat(MatrixStack m, VertexConsumerProvider vcp) {
        ModelPart core = HATS.getChild("hat7");
        ModelPart shell = HATS.getChild("hat7g");
        for (int i = 0; i < 8; i++) {
            ModelPart s = shell.getChild("p" + i);
            s.xScale = 1.22f;
            s.zScale = 1.22f;
        }
        int rgb = auraColor();
        VertexConsumer tv = vcp.getBuffer(RenderLayer.getEyes(TEX));
        core.visible = false;
        shell.visible = true;
        HATS.render(m, tv, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (0x38 << 24) | rgb);
        core.visible = true;
        shell.visible = false;
        HATS.render(m, tv, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (0xB0 << 24) | rgb);
    }

    /** Одна вершина ленты (слой Eyes: светится без теней, смешивается аддитивно). */
    static void tv(VertexConsumer vc, Matrix4f mat, float x, float y, float z, int r, int g, int b, int a) {
        vc.vertex(mat, x / 16f, y / 16f, z / 16f).color(r, g, b, a)
            .texture(20.5f / 128f, 120.5f / 256f)
            .overlay(OverlayTexture.DEFAULT_UV).light(FULL_LIGHT).normal(0f, 1f, 0f);
    }

    /** вершина плаща с текстурными координатами (атлас cape.png 256x128) */
    private static void tvt(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float u, float v, int light) {
        vc.vertex(mat, x / 16f, y / 16f, z / 16f).color(255, 255, 255, 255).texture(u / 256f, v / 128f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);
    }

    /**
     * Cape: плащ со спины. Рисованные плащи (Ace, D20, Card, Arcane, Moon) идут текстурой 40x64, а Neon светится
     * градиентом Cape Color / Cape Mix. Плащ отклоняется назад при беге и слегка колышется.
     */
    static void renderCape(MatrixStack m, VertexConsumerProvider vcp, float age, int style, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double sp = mc.player == null ? 0.0 : mc.player.getVelocity().horizontalLength();
        float target = (float) Math.min(1.0, sp * 7.0);
        long now = System.nanoTime();
        float dt = capeLastNs == 0L ? 0.016f : Math.min(0.1f, (now - capeLastNs) / 1.0e9f);
        capeLastNs = now;
        capeLean += (target - capeLean) * Math.min(1f, dt * 6f);
        float lean = capeLean;
        float t = age * 0.12f;
        int n = 10;
        float length = style == 6 ? 22f : 18f;
        float step = length / n;
        float width = style == 6 ? 12f : 11f;
        float[] cy = new float[n + 1];
        float[] cz = new float[n + 1];
        cy[0] = 0.4f;
        cz[0] = 2.3f;
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            float phi = 0.06f + lean * 0.65f * (float) Math.pow(u, 1.2) + 0.05f * (float) Math.sin(t + i * 0.5f) * (0.5f + lean);
            cy[i + 1] = cy[i] + (float) Math.cos(phi) * step;
            cz[i + 1] = cz[i] + (float) Math.sin(phi) * step;
        }
        Matrix4f mat = m.peek().getPositionMatrix();
        if (style == 6) {                                    // Neon: светящаяся лента-плащ
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEyes(TEX));
            float[][] passes = { { -0.62f, 0.62f, 0.18f }, { -0.5f, 0.5f, 0.50f }, { -0.5f, -0.40f, 1f }, { 0.40f, 0.5f, 1f } };
            for (float[] ps : passes) {
                for (int i = 0; i < n; i++) {
                    float u0 = i / (float) n, u1 = (i + 1) / (float) n;
                    float w0 = width * (1f + 0.10f * u0), w1 = width * (1f + 0.10f * u1);
                    int rgb0 = fxColor(Modules.CAPE_COLOR, Modules.CAPE_COLOR2, true, u0, 0L);
                    int rgb1 = fxColor(Modules.CAPE_COLOR, Modules.CAPE_COLOR2, true, u1, 0L);
                    int a0 = (int) (255f * ps[2] * (1f - 0.35f * u0));
                    int a1 = (int) (255f * ps[2] * (1f - 0.35f * u1));
                    float lx0 = -ps[0] * w0, rx0 = -ps[1] * w0, lx1 = -ps[0] * w1, rx1 = -ps[1] * w1;
                    tv(vc, mat, lx0, cy[i], cz[i], (rgb0 >> 16) & 255, (rgb0 >> 8) & 255, rgb0 & 255, a0);
                    tv(vc, mat, rx0, cy[i], cz[i], (rgb0 >> 16) & 255, (rgb0 >> 8) & 255, rgb0 & 255, a0);
                    tv(vc, mat, rx1, cy[i + 1], cz[i + 1], (rgb1 >> 16) & 255, (rgb1 >> 8) & 255, rgb1 & 255, a1);
                    tv(vc, mat, lx1, cy[i + 1], cz[i + 1], (rgb1 >> 16) & 255, (rgb1 >> 8) & 255, rgb1 & 255, a1);
                    tv(vc, mat, lx1, cy[i + 1], cz[i + 1], (rgb1 >> 16) & 255, (rgb1 >> 8) & 255, rgb1 & 255, a1);
                    tv(vc, mat, rx1, cy[i + 1], cz[i + 1], (rgb1 >> 16) & 255, (rgb1 >> 8) & 255, rgb1 & 255, a1);
                    tv(vc, mat, rx0, cy[i], cz[i], (rgb0 >> 16) & 255, (rgb0 >> 8) & 255, rgb0 & 255, a0);
                    tv(vc, mat, lx0, cy[i], cz[i], (rgb0 >> 16) & 255, (rgb0 >> 8) & 255, rgb0 & 255, a0);
                }
            }
            return;
        }
        // рисованный плащ: внешний слой с рисунком и внутренний тёмный слой (чтобы не просвечивало спереди)
        float u0 = (style - 1) * 40f, v0 = 0f;               // область рисунка 40x64 в cape.png
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(CAPE_TEX));
        for (int layer = 0; layer < 2; layer++) {
            float dz = layer == 0 ? 0f : 0.35f;               // внешний слой чуть дальше от тела
            for (int i = 0; i < n; i++) {
                float k0 = i / (float) n, k1 = (i + 1) / (float) n;
                float w0 = width * (1f + 0.10f * k0) * 0.5f, w1 = width * (1f + 0.10f * k1) * 0.5f;
                float ua = layer == 0 ? 208f : u0, ub = layer == 0 ? 224f : u0 + 40f;   // слева направо (на экране)
                float va = layer == 0 ? 0f : v0 + 64f * k0, vb = layer == 0 ? 16f : v0 + 64f * k1;
                if (layer == 0) { va = 0f; vb = 16f; }
                tvt(vc, mat, w0, cy[i], cz[i] + dz, ua, va, light);
                tvt(vc, mat, -w0, cy[i], cz[i] + dz, ub, va, light);
                tvt(vc, mat, -w1, cy[i + 1], cz[i + 1] + dz, ub, vb, light);
                tvt(vc, mat, w1, cy[i + 1], cz[i + 1] + dz, ua, vb, light);
            }
        }
    }

    /** Orbit Blades: светящиеся клинки кружатся вокруг тела; при беге вращаются быстрее. */
    static void renderOrbit(MatrixStack m, VertexConsumerProvider vcp, int mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int n = mode == 1 ? 3 : (mode == 2 ? 4 : 6);
        long now = System.nanoTime();
        float dt = orbitLastNs == 0L ? 0.016f : Math.min(0.1f, (now - orbitLastNs) / 1.0e9f);
        orbitLastNs = now;
        double sp = mc.player == null ? 0.0 : mc.player.getVelocity().horizontalLength();
        orbitAngle += dt * (1.6f + (float) Math.min(1.0, sp * 6.0) * 3.5f);
        Matrix4f mat = m.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEyes(TEX));
        float radius = 17f;
        for (int k = 0; k < n; k++) {
            int rgb = fxColor(Modules.ORBIT_COLOR, Modules.ORBIT_COLOR2, true, n <= 1 ? 0f : k / (float) (n - 1), k * 200L);
            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int b = rgb & 255;
            float a = orbitAngle + k * (float) (Math.PI * 2.0 / n);
            float cx = (float) Math.cos(a) * radius;
            float cz = (float) Math.sin(a) * radius;
            float cy = 6f + (k % 2 == 0 ? -3f : 3f) + (float) Math.sin(orbitAngle * 1.5f + k) * 1.5f;
            float tx = (float) -Math.sin(a);                 // касательная: направление движения клинка
            float tz = (float) Math.cos(a);
            float rx = (float) Math.cos(a);                  // радиальное направление
            float rz = (float) Math.sin(a);
            for (int pass = 0; pass < 2; pass++) {           // 0 = мягкое свечение, 1 = ядро
                float len = pass == 0 ? 9.5f : 7f;
                float tail = pass == 0 ? 6f : 4f;
                float hh = pass == 0 ? 3.6f : 2.2f;
                int al = pass == 0 ? 70 : 230;
                // два ромба-клинка: вертикальный и горизонтальный (вместе дают объём)
                float[][] v1 = { { cx + tx * len, cy, cz + tz * len }, { cx, cy - hh, cz },
                                 { cx - tx * tail, cy, cz - tz * tail }, { cx, cy + hh, cz } };
                float[][] v2 = { { cx + tx * len, cy, cz + tz * len }, { cx + rx * hh, cy, cz + rz * hh },
                                 { cx - tx * tail, cy, cz - tz * tail }, { cx - rx * hh, cy, cz - rz * hh } };
                for (float[][] v : new float[][][] { v1, v2 }) {
                    for (int i = 0; i < 4; i++) tv(vc, mat, v[i][0], v[i][1], v[i][2], r, g, b, al);
                    for (int i = 3; i >= 0; i--) tv(vc, mat, v[i][0], v[i][1], v[i][2], r, g, b, al);
                }
            }
        }
    }

    /** Хвост: цельная светящаяся лента со спины; при беге отклоняется назад, на месте слегка колышется. */
    static void renderTail(MatrixStack m, VertexConsumerProvider vcp, float age, int mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float length = mode == 2 ? 56f : 42f;
        float width = mode == 2 ? 17f : 13f;
        int n = TAIL_SEGS;
        float step = length / n;
        double sp = mc.player == null ? 0.0 : mc.player.getVelocity().horizontalLength();
        // хвост плавно отстаёт от движения: при беге вытягивается назад, на месте возвращается вниз
        float target = (float) Math.min(1.0, sp * 7.0);
        long now = System.nanoTime();
        float dt = tailLastNs == 0L ? 0.016f : Math.min(0.1f, (now - tailLastNs) / 1.0e9f);
        tailLastNs = now;
        tailLean += (target - tailLean) * Math.min(1f, dt * 6f);
        float lean = tailLean;
        float t = age * 0.12f;
        float yaw = 0.85f;                               // лента повёрнута под углом, как широкая плоскость
        float dx = (float) Math.cos(yaw);
        float dz = (float) -Math.sin(yaw);                // направление ширины ленты
        // центральная линия ленты (в системе тела, пиксели)
        float[] cx = new float[n + 1];
        float[] cy = new float[n + 1];
        float[] cz = new float[n + 1];
        cy[0] = -9f;
        cz[0] = 2.9f;
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            float phi = 0.10f + lean * 1.35f * (float) Math.pow(u, 1.3) + 0.10f * (float) Math.sin(t + i * 0.45f) * (0.4f + lean);
            cx[i + 1] = cx[i];
            cy[i + 1] = cy[i] + (float) Math.cos(phi) * step;
            cz[i + 1] = cz[i] + (float) Math.sin(phi) * step;
        }
        Matrix4f mat = m.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEyes(TEX));
        int rgb = auraColor();
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        // три прохода: широкое мягкое свечение, ядро, яркая кромка по одной стороне
        float[][] passes = { { -0.9f, 0.9f, 0.16f }, { -0.5f, 0.5f, 0.55f }, { 0.34f, 0.5f, 1.00f } };
        for (float[] ps : passes) {
            for (int i = 0; i < n; i++) {
                float u0 = i / (float) n;
                float u1 = (i + 1) / (float) n;
                float w0 = width * (1f - 0.88f * u0);
                float w1 = width * (1f - 0.88f * u1);
                int a0 = (int) (255f * ps[2] * (1f - 0.85f * u0));
                int a1 = (int) (255f * ps[2] * (1f - 0.85f * u1));
                float ax0 = cx[i] + dx * ps[0] * w0, az0 = cz[i] + dz * ps[0] * w0;
                float bx0 = cx[i] + dx * ps[1] * w0, bz0 = cz[i] + dz * ps[1] * w0;
                float ax1 = cx[i + 1] + dx * ps[0] * w1, az1 = cz[i + 1] + dz * ps[0] * w1;
                float bx1 = cx[i + 1] + dx * ps[1] * w1, bz1 = cz[i + 1] + dz * ps[1] * w1;
                // лицевая сторона
                tv(vc, mat, ax0, cy[i], az0, r, g, b, a0);
                tv(vc, mat, bx0, cy[i], bz0, r, g, b, a0);
                tv(vc, mat, bx1, cy[i + 1], bz1, r, g, b, a1);
                tv(vc, mat, ax1, cy[i + 1], az1, r, g, b, a1);
                // обратная сторона (обратный порядок вершин), чтобы лента была видна с обеих сторон
                tv(vc, mat, ax1, cy[i + 1], az1, r, g, b, a1);
                tv(vc, mat, bx1, cy[i + 1], bz1, r, g, b, a1);
                tv(vc, mat, bx0, cy[i], bz0, r, g, b, a0);
                tv(vc, mat, ax0, cy[i], az0, r, g, b, a0);
            }
        }
    }

    // ---------------------------------------------------------------- renderer

    static class CosmeticsFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        CosmeticsFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
            super(context);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            // only our own character: the rendered position must lie on our last-tick -> current-tick path
            double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
            double px = mc.player.prevX, py = mc.player.prevY, pz = mc.player.prevZ;
            double vx = cx - px, vy = cy - py, vz = cz - pz;
            double wx = state.x - px, wy = state.y - py, wz = state.z - pz;
            double len2 = vx * vx + vy * vy + vz * vz;
            double t = len2 > 1.0E-9 ? (wx * vx + wy * vy + wz * vz) / len2 : 0.0;
            if (t < 0.0) t = 0.0;
            if (t > 1.0) t = 1.0;
            double ex = wx - vx * t, ey = wy - vy * t, ez = wz - vz * t;
            if (ex * ex + ey * ey + ez * ez > 0.04) return;

            int wing = Modules.WINGS.index;
            int hat = Modules.HAT.index;
            int cubes = Modules.CUBES.index;
            int rings = Modules.AURA_RINGS.index;
            int slash = Modules.SLASH.index;
            int tail = Modules.TAIL.index;
            int orbit = Modules.ORBIT.index;
            int cape = Modules.CAPE.index;
            if (wing <= 0 && hat <= 0 && cubes <= 0 && rings <= 0 && slash <= 0 && tail <= 0 && orbit <= 0 && cape <= 0) return;

            PlayerEntityModel model = getContextModel();
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));

            if (wing > 0 && wing <= WING_STYLES) {
                int fl = Modules.WING_FLAP.index;   // 0 normal, 1 slow, 2 fast, 3 off
                float flapSpeed = fl == 1 ? 0.5f : (fl == 2 ? 1.8f : 1.0f);
                float flap = fl == 3 ? 0f : (float) Math.sin(state.age * 0.15f * flapSpeed) * 0.14f;
                float wsz = mulSize(Modules.WING_SIZE);
                for (int i = 0; i < WING_STYLES; i++) {
                    boolean on = (i == wing - 1);
                    ModelPart l = WINGS.getChild("wl" + i);
                    ModelPart r = WINGS.getChild("wr" + i);
                    l.visible = on;
                    r.visible = on;
                    if (on) {
                        l.yaw = -(0.30f + flap);
                        r.yaw = 0.30f + flap;
                        l.xScale = wsz;
                        l.yScale = wsz;
                        l.zScale = wsz;
                        r.xScale = wsz;
                        r.yScale = wsz;
                        r.zScale = wsz;
                    }
                }
                matrices.push();
                model.body.rotate(matrices);
                WINGS.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }

            if (wing > WING_STYLES) {   // неоновые крылья из геометрии: перья, перепонки, кристаллы
                int fl = Modules.WING_FLAP.index;
                float flapSpeed = fl == 1 ? 0.5f : (fl == 2 ? 1.8f : 1.0f);
                float flapA = fl == 3 ? 0f : (float) Math.sin(state.age * 0.15f * flapSpeed) * 0.14f;
                matrices.push();
                model.body.rotate(matrices);
                NeonWings.render(matrices, vertexConsumers, wing - WING_STYLES, state.age, flapA, flapSpeed,
                        mulSize(Modules.WING_SIZE));
                matrices.pop();
            }

            if (hat > 0 && hat <= HAT_STYLES) {
                for (int i = 1; i <= HAT_STYLES; i++) {
                    HATS.getChild("hat" + i).visible = (i == hat);
                }
                HATS.getChild("hat7g").visible = false;
                matrices.push();
                model.head.rotate(matrices);
                if (hat == 7) {   // China: прозрачная, светится
                    renderChinaHat(matrices, vertexConsumers);
                } else {
                    HATS.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
                }
                matrices.pop();
            }

            if (tail > 0) {
                matrices.push();
                model.body.rotate(matrices);
                renderTail(matrices, vertexConsumers, state.age, tail);
                matrices.pop();
            }

            if (cape > 0) {
                matrices.push();
                model.body.rotate(matrices);
                renderCape(matrices, vertexConsumers, state.age, cape, light);
                matrices.pop();
            }

            if (orbit > 0) {
                matrices.push();
                model.body.rotate(matrices);
                renderOrbit(matrices, vertexConsumers, orbit);
                matrices.pop();
            }

            if (cubes > 0) renderCubes(matrices, vertexConsumers, state.age, cubes);
            if (rings > 0) renderRings(matrices, vertexConsumers, state.age, rings);
            if (slash > 0) renderSlash(matrices, vertexConsumers);
        }
    }
}
