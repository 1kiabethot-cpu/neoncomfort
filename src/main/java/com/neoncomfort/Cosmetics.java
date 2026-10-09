package com.neoncomfort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
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
import net.minecraft.util.Identifier;

public class Cosmetics implements ClientModInitializer {
    static final Identifier TEX = Identifier.of("neoncomfort", "textures/entity/cosmetics.png");
    static final int WING_STYLES = 5;
    static final int HAT_STYLES = 6;
    static final int[] WING_V = {0, 21, 42, 63, 84};

    static final ModelPart WINGS = buildWings();
    static final ModelPart HATS = buildHats();

    @Override
    public void onInitializeClient() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, entityRenderer, registrationHelper, context) -> {
                if (entityRenderer instanceof PlayerEntityRenderer) {
                    registrationHelper.register(new CosmeticsFeature((PlayerEntityRenderer) entityRenderer));
                }
            });
    }

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

    static class CosmeticsFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        CosmeticsFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
            super(context);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            double dx = state.x - mc.player.getX();
            double dy = state.y - mc.player.getY();
            double dz = state.z - mc.player.getZ();
            if (dx * dx + dy * dy + dz * dz > 1.5) return;

            int wing = Modules.WINGS.index;
            int hat = Modules.HAT.index;
            if (wing <= 0 && hat <= 0) return;

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
        }
    }
                                                                    }
