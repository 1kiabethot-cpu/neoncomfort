package com.neoncomfort.mixin;

import com.neoncomfort.WorldTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sky Tint: красит цвет неба и облаков (а через них и туман). Тип результата (int / Vec3d) определяется в WorldTint. */
@Mixin(targets = "net.minecraft.client.world.ClientWorld")
public abstract class WorldColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nc$sky(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(WorldTint.tint(cir.getReturnValue(), 1.0f));
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nc$clouds(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(WorldTint.tint(cir.getReturnValue(), 0.8f));
    }
}
