package com.neoncomfort.mixin;

import com.neoncomfort.Modules;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.render.item.HeldItemRenderer")
public abstract class HeldItemMixin {
    private static final float[][] VM = {
        { 0f, 0f, 0f, 1f }, { -0.10f, -0.08f, 0f, 1f }, { 0.10f, -0.08f, 0f, 1f },
        { 0f, 0f, -0.20f, 1f }, { 0f, 0f, 0f, 0.75f }, { 0f, 0f, 0f, 1.25f }
    };

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private MatrixStack nc$viewModel(MatrixStack m) {
        int i = Modules.VIEW_MODEL.index;
        if (i <= 0 || i >= VM.length) return m;
        float[] t = VM[i];
        m.translate(t[0], t[1], t[2]);
        if (t[3] != 1f) m.scale(t[3], t[3], t[3]);
        return m;
    }
}
