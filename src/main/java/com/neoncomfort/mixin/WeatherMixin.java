package com.neoncomfort.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.neoncomfort.WorldTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rain Real Color: перекрашивает настоящий дождь (множитель цвета на время отрисовки погоды, затем возвращает белый). */
@Mixin(targets = "net.minecraft.client.render.WorldRenderer")
public abstract class WeatherMixin {
    @Inject(method = "renderWeather", at = @At("HEAD"), require = 0)
    private void nc$rainStart(CallbackInfo ci) {
        float[] c = WorldTint.rain();
        if (c != null) {
            RenderSystem.setShaderColor(c[0], c[1], c[2], 1.0f);
            WorldTint.rainTinted = true;
        }
    }

    @Inject(method = "renderWeather", at = @At("RETURN"), require = 0)
    private void nc$rainEnd(CallbackInfo ci) {
        if (WorldTint.rainTinted) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            WorldTint.rainTinted = false;
        }
    }
}
