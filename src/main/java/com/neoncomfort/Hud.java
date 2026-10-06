package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

        if (Modules.CROSSHAIR.index > 0 && mc.currentScreen == null
                && mc.options.getPerspective().isFirstPerson()) crosshair(ctx, mc);
        if (Modules.WATERMARK.enabled) watermark(ctx, mc, tr, player);
        if (Modules.COORDS.enabled) coords(ctx, mc, tr, player);
        if (Modules.ARMOR.enabled) armor(ctx, mc, tr, player);
        if (Modules.TARGET.enabled) target(ctx, mc, tr);
        if (Modules.NEARBY_ARMOR.enabled) nearby(ctx, mc, tr, player);
    }

    private static void crosshair(DrawContext ctx, MinecraftClient mc) {
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int c = Theme.crosshairColor();
        switch (Modules.CROSSHAIR.index) {
            case 1 -> ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, c);
            case 2 -> {
                ctx.fill(cx - 7, cy - 1, cx - 2, cy + 1, c);
                ctx.fill(cx + 2, cy - 1, cx + 7, cy + 1, c);
                ctx.fill(cx - 1, cy - 7, cx + 1, cy - 2, c);
                ctx.fill(cx - 1, cy + 2, cx + 1, cy + 7, c);
            }
            case 3 -> {
                ctx.fill(cx - 6, cy - 1, cx + 6, cy + 1, c);
                ctx.fill(cx - 1, cy - 6, cx + 1, cy + 6, c);
            }
            case 4 -> {
                ctx.fill(cx - 5, cy - 5, cx + 5, cy - 4, c);
                ctx.fill(cx - 5, cy + 4, cx + 5, cy + 5, c);
                ctx.fill(cx - 5, cy - 4, cx - 4, cy + 4, c);
                ctx.fill(cx + 4, cy - 4, cx + 5, cy + 4, c);
            }
            default -> { }
        }
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

    /** Броня справа от хотбара: число = оставшаяся прочность, рядом иконка. */
    private static void armor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        int x = mc.getWindow().getScaledWidth() / 2 + 148;
        int y = mc.getWindow().getScaledHeight() - 78;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack st = p.getEquippedStack(SLOTS[i]);
            int yy = y + i * 18;
            if (st.isEmpty()) continue;
            ctx.drawItem(st, x, yy);
            if (st.getMaxDamage() > 0) {
                int left = st.getMaxDamage() - st.getDamage();
                int pct = left * 100 / st.getMaxDamage();
                String num = String.valueOf(left);
                int col = pct > 20 ? 0xFFFFFFFF : 0xFFFF5555;
                ctx.drawText(tr, num, x - 3 - tr.getWidth(num), yy + 4, col, true);
            }
        }
    }

    /** Броня ближайших игроков (до 3, в радиусе 32 блоков). */
    private static void nearby(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity self) {
        if (mc.world == null) return;
        List<PlayerEntity> list = new ArrayList<>();
        for (PlayerEntity o : mc.world.getPlayers()) {
            if (o == self || o.isSpectator()) continue;
            if (self.distanceTo(o) <= 32f) list.add(o);
        }
        if (list.isEmpty()) return;
        list.sort(Comparator.comparingDouble(self::distanceTo));
        if (list.size() > 3) list = list.subList(0, 3);

        int blockH = 36, gap = 4;
        int total = list.size() * (blockH + gap) - gap;
        int y = mc.getWindow().getScaledHeight() / 2 - total / 2;
        for (PlayerEntity o : list) {
            String title = o.getName().getString() + "  " + Math.round(self.distanceTo(o)) + "m";
            int w = Math.max(tr.getWidth(title), SLOTS.length * 20) + 14;
            box(ctx, 6, y, w, blockH);
            ctx.drawText(tr, title, 14, y + 4, Theme.TEXT, true);
            for (int j = 0; j < SLOTS.length; j++) {
                ItemStack st = o.getEquippedStack(SLOTS[j]);
                int ix = 14 + j * 20;
                if (st.isEmpty()) {
                    ctx.fill(ix, y + 14, ix + 16, y + 30, 0x22FFFFFF);
                    continue;
                }
                ctx.drawItem(st, ix, y + 14);
                if (st.getMaxDamage() > 0) {
                    int left = st.getMaxDamage() - st.getDamage();
                    int pct = left * 100 / st.getMaxDamage();
                    int col = pct > 50 ? 0xFF55FF55 : pct > 20 ? 0xFFFFFF55 : 0xFFFF5555;
                    ctx.fill(ix, y + 31, ix + 16, y + 33, Theme.OFF);
                    ctx.fill(ix, y + 31, ix + Math.max(1, 16 * pct / 100), y + 33, col);
                }
            }
            y += blockH + gap;
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
        ctx.fill(x + 8, y + 17, x + 8 + fill, y + 21, Theme.accent());
    }

    private Hud() {}
}
