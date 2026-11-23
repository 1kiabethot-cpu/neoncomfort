package com.neoncomfort.mixin;

import com.neoncomfort.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overlay Cleaner: убирает накладки огня, воды и «в стене» на экране. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class OverlayMixin {
    @Inject(method = "renderOverlays", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nc$noOverlays(CallbackInfo ci) {
        if (Modules.OVERLAY_CLEAN.enabled) ci.cancel();
    }
}
