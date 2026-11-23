package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Небольшие информационные функции HUD: Info Panel, Item Counter, Armor Warning, Low HP Pulse, Biome Title. Без иконок предметов (дёшево). */
final class InfoHud {
    private static final List<String> ROWS = new ArrayList<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static int frame = 0;
    private static int bframe = 0;
    private static int totems, pearls, gapples, xp, rockets;
    private static String lastBiome = "";
    private static String biomeShown = "";
    private static long biomeAt = 0L;

    private static void count(ClientPlayerEntity p) {
        totems = 0; pearls = 0; gapples = 0; xp = 0; rockets = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isEmpty()) continue;
            if (st.getItem() == Items.TOTEM_OF_UNDYING) totems += st.getCount();
            else if (st.getItem() == Items.ENDER_PEARL) pearls += st.getCount();
            else if (st.getItem() == Items.GOLDEN_APPLE || st.getItem() == Items.ENCHANTED_GOLDEN_APPLE) gapples += st.getCount();
            else if (st.getItem() == Items.EXPERIENCE_BOTTLE) xp += st.getCount();
            else if (st.getItem() == Items.FIREWORK_ROCKET) rockets += st.getCount();
        }
    }

    /** Панель слева: скорость, время сессии, убийства/смерти, пересчёт координат Незера и количество важных предметов. */
    static float panel(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p, float y, boolean streamer) {
        ROWS.clear();
        if (Modules.INFO_PANEL.enabled && !streamer) {
            double sp = p.getVelocity().horizontalLength() * 20.0;
            ROWS.add(Lang.t("Speed") + "  " + (Math.round(sp * 10.0) / 10.0) + " b/s");
            long sec = (System.currentTimeMillis() - Nav.sessionStart) / 1000L;
            ROWS.add(Lang.t("Session") + "  " + (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60));
            ROWS.add("K " + Nav.kills + "   D " + Nav.deaths);
            String dim = Nav.dimOf(mc);
            if (dim.endsWith("overworld")) ROWS.add(Lang.t("Nether") + "  " + (int) (p.getX() / 8) + " " + (int) (p.getZ() / 8));
            else if (dim.endsWith("the_nether")) ROWS.add(Lang.t("Overworld") + "  " + (int) (p.getX() * 8) + " " + (int) (p.getZ() * 8));
        }
        if (Modules.ITEM_COUNTER.enabled) {
            if (frame++ % 10 == 0) count(p);
            if (totems > 0) ROWS.add(Lang.t("Totem") + "  " + totems);
            if (pearls > 0) ROWS.add(Lang.t("Pearl") + "  " + pearls);
            if (gapples > 0) ROWS.add(Lang.t("Gapple") + "  " + gapples);
            if (xp > 0) ROWS.add("XP  " + xp);
            if (rockets > 0) ROWS.add(Lang.t("Rocket") + "  " + rockets);
        }
        if (ROWS.isEmpty()) return y;
        int w = 96;
        for (String r : ROWS) w = Math.max(w, tr.getWidth(r) + 14);
        int h = 6 + ROWS.size() * 11 + 3;
        Hud.begin(ctx, 6, y);
        Hud.box(ctx, 0, 0, w, h);
        int yy = 6;
        for (String r : ROWS) {
            ctx.drawText(tr, r, 6, yy, Theme.TEXT, true);
            yy += 11;
        }
        Hud.end(ctx);
        return y + h * Hud.S() + 4;
    }

    /** Armor Warning: если у какой-то брони осталось меньше 15% прочности, по центру мигает предупреждение. */
    static void armorWarn(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        boolean low = false;
        for (EquipmentSlot s : ARMOR_SLOTS) {
            ItemStack st = p.getEquippedStack(s);
            if (!st.isEmpty() && st.getMaxDamage() > 0 && st.getMaxDamage() - st.getDamage() < st.getMaxDamage() * 0.15f) low = true;
        }
        if (!low || (!Modules.COMFORT.enabled && (System.currentTimeMillis() / 350L) % 2 != 0)) return;
        ctx.drawCenteredTextWithShadow(tr, Lang.t("Armor low"), mc.getWindow().getScaledWidth() / 2,
                mc.getWindow().getScaledHeight() / 5, 0xFFFF4444);
    }

    /** Low HP Pulse: при низком здоровье края экрана мягко пульсируют красным. */
    static void lowHp(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        float hp = p.getHealth();
        if (hp > 6f || hp <= 0f) return;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / (Modules.COMFORT.enabled ? 900.0 : (hp <= 3f ? 160.0 : 280.0)));
        int base = 30 + (int) (pulse * (Modules.COMFORT.enabled ? 40f : 90f));
        int eh = h / 6, ew = w / 10;
        ctx.fillGradient(0, 0, w, eh, (base << 24) | 0xC01010, 0x00C01010);
        ctx.fillGradient(0, h - eh, w, h, 0x00C01010, (base << 24) | 0xC01010);
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            int a = (int) (base * (1f - i / (float) steps));
            int x0 = i * ew / steps, x1 = (i + 1) * ew / steps;
            ctx.fill(x0, 0, x1, h, (a << 24) | 0xC01010);
            ctx.fill(w - x1, 0, w - x0, h, (a << 24) | 0xC01010);
        }
    }

    private static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : id.toCharArray()) {
            if (c == '_') { sb.append(' '); up = true; }
            else { sb.append(up ? Character.toUpperCase(c) : c); up = false; }
        }
        return sb.toString();
    }

    /** Biome Title: при входе в новый биом по центру на 3,5 секунды появляется его название. */
    static void biome(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        if (mc.world == null) return;
        if (bframe++ % 20 == 0) {
            String id = mc.world.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("");
            if (!id.isEmpty() && !id.equals(lastBiome)) {
                if (!lastBiome.isEmpty()) {
                    biomeShown = pretty(id);
                    biomeAt = System.currentTimeMillis();
                }
                lastBiome = id;
            }
        }
        long age = System.currentTimeMillis() - biomeAt;
        if (biomeShown.isEmpty() || age > 3500L) return;
        int a = age < 500L ? (int) (255 * age / 500f) : (age > 2800L ? (int) (255 * (3500L - age) / 700f) : 255);
        if (a < 8) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        ctx.getMatrices().push();
        ctx.getMatrices().translate(sw / 2f, sh * 0.30f, 0f);
        ctx.getMatrices().scale(2f, 2f, 1f);
        ctx.drawCenteredTextWithShadow(tr, biomeShown, 0, 0, (a << 24) | 0xFFFFFF);
        ctx.getMatrices().pop();
    }

    private InfoHud() {}
}
