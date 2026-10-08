package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
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

        if (Modules.TWILIGHT.index > 0) twilight(ctx, mc, Modules.TWILIGHT.index, Modules.TWILIGHT_STYLE.index);
        ScreenFx.render(ctx, mc);
        if (mc.currentScreen == null) hitMarker(ctx, mc);
        if (Modules.CROSSHAIR.index < Modules.CROSSHAIR.values.length - 1 && mc.currentScreen == null
                && mc.options.getPerspective().isFirstPerson()) crosshair(ctx, mc);
        if (Modules.WATERMARK.enabled) watermark(ctx, mc, tr, player);
        if (Modules.COORDS.enabled) coords(ctx, mc, tr, player);
        if (Modules.ARMOR.enabled) armor(ctx, mc, tr, player);
        if (Modules.TARGET.enabled) target(ctx, mc, tr);
        if (Modules.NEARBY_ARMOR.enabled) nearby(ctx, mc, tr, player);
    }

    private static void dot(DrawContext ctx, int x, int y, int c) {
        ctx.fill(x - 1, y - 1, x + 1, y + 1, c);
    }

    private static void rect(DrawContext ctx, int xa, int ya, int xb, int yb, int c) {
        ctx.fill(Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb), Math.max(ya, yb), c);
    }

    private static void ring(DrawContext ctx, int cx, int cy, int r, int pts, int c) {
        for (int i = 0; i < pts; i++) {
            double a = Math.PI * 2 * i / pts;
            dot(ctx, cx + (int) Math.round(Math.cos(a) * r), cy + (int) Math.round(Math.sin(a) * r), c);
        }
    }

    /** Hit Marker: короткий спалах диагональных черт вокруг прицела при попадании. */
    private static void hitMarker(DrawContext ctx, MinecraftClient mc) {
        int mk = Modules.HIT_MARKER.index;
        if (mk <= 0) return;
        long dt = System.currentTimeMillis() - NeonComfort.lastHitMs;
        if (dt < 0 || dt > 260) return;
        int a = (int) (255 * (1 - dt / 260f));
        int c = (a << 24) | Theme.colorRgb(mk - 1);
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int i = 5; i <= 9; i++) dot(ctx, cx + sx * i, cy + sy * i, c);
            }
        }
    }

    private static final float[] CH_SCALE = { 1f, 0.75f, 1.5f, 2f };   // Normal, Small, Big, Huge

    private static void crosshair(DrawContext ctx, MinecraftClient mc) {
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int c = Theme.crosshairColor();
        int style = Modules.CROSSHAIR.index;
        float k = CH_SCALE[Modules.CROSSHAIR_SIZE.index % CH_SCALE.length];

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate((float) cx, (float) cy, 0f);
        m.scale(k, k, 1f);
        if (Modules.CROSSHAIR_OUTLINE.enabled) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox != 0 || oy != 0) shape(ctx, style, ox, oy, 0xFF000000);
                }
            }
        }
        shape(ctx, style, 0, 0, c);
        m.pop();
    }

    private static void shape(DrawContext ctx, int style, int cx, int cy, int c) {
        switch (style) {
            case 0 -> {                                                   // Thin
                ctx.fill(cx - 7, cy, cx - 2, cy + 1, c);
                ctx.fill(cx + 2, cy, cx + 7, cy + 1, c);
                ctx.fill(cx, cy - 7, cx + 1, cy - 2, c);
                ctx.fill(cx, cy + 2, cx + 1, cy + 7, c);
            }
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
            case 5 -> ring(ctx, cx, cy, 6, 20, c);                       // Circle
            case 6 -> {                                                   // Diamond
                for (int i = 0; i <= 6; i += 2) {
                    dot(ctx, cx + i, cy + (6 - i), c);
                    dot(ctx, cx - i, cy + (6 - i), c);
                    dot(ctx, cx + i, cy - (6 - i), c);
                    dot(ctx, cx - i, cy - (6 - i), c);
                }
            }
            case 7 -> {                                                   // X
                for (int i = 2; i <= 6; i++) {
                    dot(ctx, cx + i, cy + i, c);
                    dot(ctx, cx - i, cy + i, c);
                    dot(ctx, cx + i, cy - i, c);
                    dot(ctx, cx - i, cy - i, c);
                }
            }
            case 8 -> {                                                   // Corners
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int x0 = cx + sx * 6, y0 = cy + sy * 6;
                        rect(ctx, x0, y0, x0 - sx * 4, y0 + sy, c);
                        rect(ctx, x0, y0, x0 + sx, y0 - sy * 4, c);
                    }
                }
            }
            case 9 -> {                                                   // Plus Dot
                ctx.fill(cx - 7, cy - 1, cx - 3, cy + 1, c);
                ctx.fill(cx + 3, cy - 1, cx + 7, cy + 1, c);
                ctx.fill(cx - 1, cy - 7, cx + 1, cy - 3, c);
                ctx.fill(cx - 1, cy + 3, cx + 1, cy + 7, c);
                dot(ctx, cx, cy, c);
            }
            case 10 -> {                                                  // Ring Dot
                ring(ctx, cx, cy, 5, 16, c);
                dot(ctx, cx, cy, c);
            }
            case 11 -> {                                                  // T-Shape
                ctx.fill(cx - 7, cy - 1, cx - 2, cy + 1, c);
                ctx.fill(cx + 2, cy - 1, cx + 7, cy + 1, c);
                ctx.fill(cx - 1, cy + 2, cx + 1, cy + 7, c);
                dot(ctx, cx, cy, c);
            }
            case 12 -> {                                                  // Big Ring
                ring(ctx, cx, cy, 9, 28, c);
                dot(ctx, cx, cy, c);
            }
            case 13 -> {                                                  // Brackets
                ctx.fill(cx - 7, cy - 6, cx - 6, cy + 6, c);
                ctx.fill(cx - 7, cy - 6, cx - 3, cy - 5, c);
                ctx.fill(cx - 7, cy + 5, cx - 3, cy + 6, c);
                ctx.fill(cx + 6, cy - 6, cx + 7, cy + 6, c);
                ctx.fill(cx + 3, cy - 6, cx + 7, cy - 5, c);
                ctx.fill(cx + 3, cy + 5, cx + 7, cy + 6, c);
                dot(ctx, cx, cy, c);
            }
            case 14 -> {                                                  // Star
                dot(ctx, cx, cy, c);
                for (int i = 3; i <= 7; i += 2) {
                    dot(ctx, cx + i, cy, c);
                    dot(ctx, cx - i, cy, c);
                    dot(ctx, cx, cy + i, c);
                    dot(ctx, cx, cy - i, c);
                }
                for (int i = 3; i <= 5; i += 2) {
                    dot(ctx, cx + i, cy + i, c);
                    dot(ctx, cx - i, cy + i, c);
                    dot(ctx, cx + i, cy - i, c);
                    dot(ctx, cx - i, cy - i, c);
                }
            }
            default -> { }
        }
    }

    /** Twilight: градиент поверх экрана (без нагрузки на FPS). style: 0 Sunset, 1 Pink, 2 Aurora, 3 Violet, 4 Gold */
    private static void twilight(DrawContext ctx, MinecraftClient mc, int level, int style) {
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        int a = level == 1 ? 0x28 : level == 2 ? 0x44 : 0x60;
        if (style == 2) { aurora(ctx, w, h, a); return; }
        int top, mid, bot;
        switch (style) {
            case 1 -> { top = 0xFFB6C8; mid = 0xFF8FB0; bot = 0x5A2A50; }
            case 3 -> { top = 0x1B1464; mid = 0x7A3CC8; bot = 0x120A3A; }
            case 4 -> { top = 0xFF9A3C; mid = 0xFFD27A; bot = 0x4A2A10; }
            default -> { top = 0x4A2A8C; mid = 0xFF8A3D; bot = 0x2A1250; }
        }
        ctx.fillGradient(0, 0, w, h / 2, (a << 24) | top, (a << 24) | mid);
        ctx.fillGradient(0, h / 2, w, h, (a << 24) | mid, (a << 24) | bot);
    }

    /** Северное сияние: тёмный фон и переливающиеся полосы сверху. */
    private static void aurora(DrawContext ctx, int w, int h, int a) {
        ctx.fill(0, 0, w, h, ((a / 2) << 24) | 0x0A1030);
        double t = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 5; i++) {
            int y0 = (int) (h * (0.02 + i * 0.08));
            int y1 = y0 + (int) (h * 0.16);
            int ym = (y0 + y1) / 2;
            double k = 0.35 + 0.65 * Math.abs(Math.sin(t * 0.6 + i * 1.3));
            int al = (int) (a * k);
            int rgb = (i % 2 == 0) ? 0x3CFFA0 : 0xB070FF;
            ctx.fillGradient(0, y0, w, ym, rgb, (al << 24) | rgb);
            ctx.fillGradient(0, ym, w, y1, (al << 24) | rgb, rgb);
        }
    }

    /** общий масштаб HUD (0.8 = на 20% меньше) */
    private static final float[] HUD_SCALES = { 0.8f, 0.7f, 0.9f, 1.0f, 1.2f };

    private static float S() {
        return HUD_SCALES[Modules.HUD_SIZE.index % HUD_SCALES.length];
    }

    private static void begin(DrawContext ctx, float ax, float ay) {
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(ax, ay, 0f);
        float sc = S();
        m.scale(sc, sc, 1f);
    }

    private static void end(DrawContext ctx) {
        ctx.getMatrices().pop();
    }

    private static void box(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, Theme.BG);
        ctx.fill(x, y, x + 2, y + h, Theme.pulsingAccent());
    }

    private static void watermark(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        StringBuilder sb = new StringBuilder("NeonComfort v" + NeonComfort.VERSION + "  |  ").append(mc.getCurrentFps()).append(" FPS");
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
        begin(ctx, 6, 6);
        box(ctx, 0, 0, w, 16);
        ctx.drawText(tr, s, 6, 4, Theme.TEXT, true);
        end(ctx);
    }

    private static void coords(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        String l1 = "XYZ  " + p.getBlockX() + "  " + p.getBlockY() + "  " + p.getBlockZ();
        String l2 = "Facing  " + p.getHorizontalFacing().asString();
        int w = Math.max(tr.getWidth(l1), tr.getWidth(l2)) + 12;
        int h = 28;
        float y = mc.getWindow().getScaledHeight() - h * S() - 6;
        begin(ctx, 6, y);
        box(ctx, 0, 0, w, h);
        ctx.drawText(tr, l1, 6, 5, Theme.TEXT, true);
        ctx.drawText(tr, l2, 6, 16, Theme.TEXT_DIM, true);
        end(ctx);
    }

    /** Броня справа от хотбара: число = оставшаяся прочность, рядом иконка. */
    private static void armor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, ClientPlayerEntity p) {
        int x = mc.getWindow().getScaledWidth() / 2 + 148;
        int y = mc.getWindow().getScaledHeight() - 78;
        begin(ctx, x, y);
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack st = p.getEquippedStack(SLOTS[i]);
            int yy = i * 18;
            if (st.isEmpty()) continue;
            ctx.drawItem(st, 0, yy);
            if (st.getMaxDamage() > 0) {
                int left = st.getMaxDamage() - st.getDamage();
                int pct = left * 100 / st.getMaxDamage();
                String num = String.valueOf(left);
                int col = pct > 20 ? 0xFFFFFFFF : 0xFFFF5555;
                ctx.drawText(tr, num, -3 - tr.getWidth(num), yy + 4, col, true);
            }
        }
        end(ctx);
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
        float top = mc.getWindow().getScaledHeight() / 2f - total * S() / 2f;
        begin(ctx, 6, top);
        int y = 0;
        for (PlayerEntity o : list) {
            String title = o.getName().getString() + "  " + Math.round(self.distanceTo(o)) + "m";
            int w = Math.max(tr.getWidth(title), SLOTS.length * 20) + 14;
            box(ctx, 0, y, w, blockH);
            ctx.drawText(tr, title, 8, y + 4, Theme.TEXT, true);
            for (int j = 0; j < SLOTS.length; j++) {
                ItemStack st = o.getEquippedStack(SLOTS[j]);
                int ix = 8 + j * 20;
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
        end(ctx);
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
        float x = mc.getWindow().getScaledWidth() / 2f - w * S() / 2f;
        begin(ctx, x, 28);
        box(ctx, 0, 0, w, 26);
        ctx.drawText(tr, txt, 8, 5, Theme.TEXT, true);
        int barW = w - 16;
        ctx.fill(8, 17, 8 + barW, 21, Theme.OFF);
        int fill = (int) (barW * Math.min(1f, hp / max));
        ctx.fill(8, 17, 8 + fill, 21, Theme.accent());
        end(ctx);
    }

    private Hud() {}
}
