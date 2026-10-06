package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Hud {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;

        if (Modules.WATERMARK.enabled) watermark(ctx, mc, tr, player);
        if (Modules.COORDS.enabled) coords(ctx, mc, tr, player);
        if (Modules.ARMOR.enabled) armor(ctx, mc, tr, player);
        if (Modules.TARGET.enabled) target(ctx, mc, tr);
    }

    private static void box(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, Theme.BG);
        ctx.fill(x, y, x + 2, y + h, Theme.pulsingAccent());
    }

    private static void watermark(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        StringBuilder sb = new StringBuilder("NeonComfort  |  ").append(mc.getCurrentFps()).append(" FPS");
        if (Modules.PING_TIME.enabled) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (e != null) ping = e.getLatency();
            }
            sb.append("  |  ").append(ping).append(" ms  |  ").append(LocalTime.now().format(TIME));
        }
        String s = sb.toString();
        int w = tr.getWidth(s) + 12;
        box(ctx, 6, 6, w, 16);
        ctx.drawText(tr, s, 12, 10, Theme.TEXT, true);
    }

    private static void coords(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        String l1 = "XYZ  " + p.getBlockX() + "  " + p.getBlockY() + "  " + p.getBlockZ();
        String l2 = "Facing  " + p.getHorizontalFacing().asString();
        int w = Math.max(tr.getWidth(l1), tr.getWidth(l2)) + 12;
        int h = 28;
        int y = mc.getWindow().getScaledHeight() - h - 6;
        box(ctx, 6, y, w, h);
        ctx.drawText(tr, l1, 12, y + 5, Theme.TEXT, true);
        ctx.drawText(tr, l2, 12, y + 16, Theme.TEXT_DIM, true);
    }

    private static void armor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        int x = mc.getWindow().getScaledWidth() - 66;
        int y = mc.getWindow().getScaledHeight() / 2 - 40;
        box(ctx, x - 4, y - 4, 66, 88);
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack st = p.getEquippedStack(SLOTS[i]);
            int yy = y + i * 21;
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, yy);
            if (st.getMaxDamage() > 0) {
                int left = st.getMaxDamage() - st.getDamage();
                int pct = left * 100 / st.getMaxDamage();
                int col = pct > 50 ? 0xFF55FF55 : pct > 20 ? 0xFFFFFF55 : 0xFFFF5555;
                ctx.drawText(tr, pct + "%", x + 20, yy + 4, col, true);
            }
        }
    }

    private static void target(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof LivingEntity le)) return;
        String name = le.getName().getString();
        float hp = le.getHealth();
        float max = Math.max(1f, le.getMaxHealth());
        String txt = name + "  " + String.format("%.1f", hp) + " / " + String.format("%.0f", max);
        int w = Math.max(110, tr.getWidth(txt) + 14);
        int x = mc.getWindow().getScaledWidth() / 2 - w / 2;
        int y = 28;
        box(ctx, x, y, w, 26);
        ctx.drawText(tr, txt, x + 8, y + 5, Theme.TEXT, true);
        int barW = w - 16;
        ctx.fill(x + 8, y + 17, x + 8 + barW, y + 21, Theme.OFF);
        int fill = (int) (barW * Math.min(1f, hp / max));
        ctx.fill(x + 8, y + 17, x + 8 + fill, y + 21, Theme.ACCENT);
    }

    private Hud() {}
    }
