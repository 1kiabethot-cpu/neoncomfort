package com.neoncomfort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class NeonComfort implements ClientModInitializer {
    private static final String CATEGORY = "category.neoncomfort";

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;

    private boolean zooming = false;
    private int savedFov = 70;
    private boolean fullbrightWasOn = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neoncomfort.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neoncomfort.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // Кнопка в меню паузы — удобно на телефоне, где нет RShift
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("NeonComfort"),
                                        b -> client.setScreen(new MenuScreen()))
                                .dimensions(6, 6, 100, 20).build());
            }
        });
    }

    private void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new MenuScreen());
        }

        handleZoom(mc);
        handleFullbright(p);
        handleAutoSprint(mc, p);
        handleWeather(mc);
        handleTrail(mc, p);
    }

    /** Цветной хвост за персонажем, когда ты идёшь / бежишь (только визуально, на твоём клиенте). */
    private void handleTrail(MinecraftClient mc, ClientPlayerEntity p) {
        int mode = Modules.TRAIL.index;
        if (mode == 0 || mc.world == null) return;
        double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
        double sp = Math.sqrt(dx * dx + dz * dz);
        if (sp < 0.04) return;

        int rgb = (mode == Theme.colorCount() + 1) ? Theme.rainbowRgb() : Theme.colorRgb(mode - 1);
        DustParticleEffect fx = new DustParticleEffect(rgb, 1.2f);
        double nx = dx / sp, nz = dz / sp;
        for (int i = 0; i < 3; i++) {
            double t = i / 3.0;
            double x = p.prevX + dx * t - nx * 0.3;
            double z = p.prevZ + dz * t - nz * 0.3;
            double y = p.getY() + 0.4 + Math.random() * 0.8;
            mc.world.addParticle(fx, x, y, z, 0, 0, 0);
        }
    }

    /** Погода только на клиенте: видишь только ты, сервер не меняется. */
    private void handleWeather(MinecraftClient mc) {
        ClientWorld w = mc.world;
        int mode = Modules.WEATHER.index;
        if (w == null || mode == 0) return;
        w.setRainGradient(mode >= 2 ? 1f : 0f);
        w.setThunderGradient(mode == 3 ? 1f : 0f);
    }

    private void handleZoom(MinecraftClient mc) {
        boolean want = Modules.ZOOM.enabled && zoomKey.isPressed() && mc.currentScreen == null;
        if (want && !zooming) {
            savedFov = mc.options.getFov().getValue();
            mc.options.getFov().setValue(30);
            zooming = true;
        } else if (!want && zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private void handleFullbright(ClientPlayerEntity p) {
        if (Modules.FULLBRIGHT.enabled) {
            p.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            fullbrightWasOn = true;
        } else if (fullbrightWasOn) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            fullbrightWasOn = false;
        }
    }

    private void handleAutoSprint(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.AUTOSPRINT.enabled) return;
        if (mc.options.forwardKey.isPressed()
                && !p.isSneaking()
                && !p.horizontalCollision
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
