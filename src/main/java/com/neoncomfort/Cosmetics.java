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

public class Cosmetics implements ClientModInitializer {
    static final Identifier TEX = Identifier.of("neoncomfort", "textures/entity/cosmetics.png");
    static final int WING_STYLES = 5;
    static final int HAT_STYLES = 6;
    static final int[] WING_V = {0, 21, 42, 63, 84};
    static final int MAX_CUBES = 10;
    static final int RING_SEGS = 18;
    static final int MAX_RINGS = 3;
    static final int FULL_LIGHT = 0xF000F0;

    static final ModelPart WINGS = buildWings();
    static final ModelPart HATS = buildHats();
    static final ModelPart CUBES = buildCubes();
    static final ModelPart RINGS = buildRings();

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
        return TexturedModelData.of(data, 128, 128).createModel();
    }

    private static ModelPart buildCubes() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (int i = 0; i < MAX_CUBES; i++) {
            root.addChild("c" + i, ModelPartBuilder.create().uv(0, 108).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
                ModelTransform.NONE);
        }
        return TexturedModelData.of(data, 128, 128).createModel();
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
        return TexturedModelData.of(data, 128, 128).createModel();
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
        return TexturedModelData.of(data, 128, 128).createModel();
    }

    // ---------------------------------------------------------------- colors

    static int auraColor() {
        int idx = Modules.AURA_COLOR.index;
        if (idx <= 0) return Theme.accent() & 0xFFFFFF;
        if (idx == Theme.colorCount() + 1) return Theme.rainbowRgb() & 0xFFFFFF;
        return Theme.colorRgb(idx - 1) & 0xFFFFFF;
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
        float spin = age * 0.05f;
        float t = age * 0.04f;
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
            cube.pivotX = x;
            cube.pivotY = y;
            cube.pivotZ = z;
            cube.yaw = a * 2f;
            cube.pitch = a;
        }
        int look = Modules.CUBE_LOOK.index;       // 0 solid, 1 glass, 2 glow
        int rgb = auraColor();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEX));
        if (look == 2) {
            setCubeScale(2.2f);
            CUBES.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 0x38000000 | rgb);
        }
        setCubeScale(1f);
        int alpha = look == 0 ? 0xE0 : (look == 1 ? 0x70 : 0xD0);
        CUBES.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, (alpha << 24) | rgb);
    }

    // ---------------------------------------------------------------- rings

    static void renderRings(MatrixStack m, VertexConsumerProvider vcp, float age, int mode) {
        // mode: 1 = one moving ring, 2 = three moving rings, 3 = magic circle on the ground
        float spin = age * 0.06f;
        int count = mode == 2 ? 3 : (mode == 1 ? 1 : 2);
        for (int r = 0; r < MAX_RINGS; r++) {
            boolean on = r < count;
            float radius;
            float y;
            float rot;
            if (mode == 3) {
                radius = r == 0 ? 15f : 9f;
                y = 23.6f;
                rot = r == 0 ? spin : -spin * 1.4f;
            } else {
                radius = 10f;
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
        RINGS.render(m, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 0xC0000000 | auraColor());
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
        if (mode == 1) {
            hitRing(mc, cx, y0 + h * 0.5, cz, 0.13, r, g, b);
        } else if (mode == 2) {
            hitRing(mc, cx, y0 + h * 0.25, cz, 0.13, r, g, b);
            hitRing(mc, cx, y0 + h * 0.75, cz, 0.13, r, g, b);
        } else {
            for (int i = 0; i < 28; i++) {
                double vx = Math.random() * 2.0 - 1.0;
                double vy = Math.random() * 2.0 - 1.0;
                double vz = Math.random() * 2.0 - 1.0;
                double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (len < 1.0E-3) continue;
                double sp = 0.12 / len;
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
            if (wing <= 0 && hat <= 0 && cubes <= 0 && rings <= 0) return;

            PlayerEntityModel model = getContextModel();
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));

            if (wing > 0 && wing <= WING_STYLES) {
                float flap = (float) Math.sin(state.age * 0.15f) * 0.14f;
                for (int i = 0; i < WING_STYLES; i++) {
                    boolean on = (i == wing - 1);
                    ModelPart l = WINGS.getChild("wl" + i);
                    ModelPart r = WINGS.getChild("wr" + i);
                    l.visible = on;
                    r.visible = on;
                    if (on) {
                        l.yaw = -(0.30f + flap);
                        r.yaw = 0.30f + flap;
                    }
                }
                matrices.push();
                model.body.rotate(matrices);
                WINGS.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }

            if (hat > 0 && hat <= HAT_STYLES) {
                for (int i = 1; i <= HAT_STYLES; i++) {
                    HATS.getChild("hat" + i).visible = (i == hat);
                }
                matrices.push();
                model.head.rotate(matrices);
                HATS.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }

            if (cubes > 0) renderCubes(matrices, vertexConsumers, state.age, cubes);
            if (rings > 0) renderRings(matrices, vertexConsumers, state.age, rings);
        }
    }
                   }
