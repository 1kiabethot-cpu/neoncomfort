package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Звуки меню. Набор выбирается в Menu Sound; высота тона зависит от действия (открыть, выбрать, выключить, вкладка, закрыть). */
public final class UiSound {
    public static final int OPEN = 0, PICK = 1, OFF = 2, TAB = 3, CLOSE = 4;

    //                                  OFF   Click                 Soft                    Pop                              Chime                       Crystal
    private static final String[] ID = { null, "ui.button.click", "block.note_block.hat", "entity.experience_orb.pickup", "block.note_block.chime", "block.amethyst_block.chime" };
    private static final float[] VOL = { 0f, 0.55f, 0.45f, 0.20f, 0.55f, 0.50f };
    private static final float[] BASE = { 1f, 1.0f, 1.3f, 1.25f, 1.0f, 1.0f };
    private static final float[] KIND = { 1.0f, 1.25f, 0.80f, 1.12f, 0.72f };

    public static void play(int kind) {
        int m = Modules.MENU_SOUND.index;
        if (m <= 0 || m >= ID.length) return;
        float pitch = Math.max(0.5f, Math.min(2.0f, BASE[m] * KIND[kind % KIND.length]));
        SoundEvent ev = SoundEvent.of(Identifier.ofVanilla(ID[m]));
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, pitch, VOL[m]));
    }

    private UiSound() {}
}
