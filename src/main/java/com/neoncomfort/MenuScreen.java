package com.neoncomfort;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню 2.17: скруглённые карточки, вкладки слева (на узком экране полоса со стрелками), прокрутка пальцем.
 * Тап по пункту с несколькими значениями открывает окно выбора: список значений или квадратная палитра цветов.
 * Переключатели (вкл/выкл) меняются сразу. Звуки меню настраиваются в Menu Sound.
 */
public class MenuScreen extends Screen {
    private static final int HEAD = 28, FOOT = 14, ROW = 24, GAP = 3, STRIP = 20, CHIP_H = 16;
    private static final int POP_ROW = 18, TILE = 22, TGAP = 4;
    private static final String[] PAGE_NAMES = { "Main", "HUD", "World", "Combat", "Movement", "Aura", "Cosmetics", "Team", "Info", "Sky", "Visuals" };

    private static int lastTab = 0;
    private int tab = lastTab;
    private float scroll = 0f;

    private final long openMs = System.currentTimeMillis();
    private long lastNs = 0L;
    private final Map<Modules.Module, Float> glow = new HashMap<>();
    private final Map<Modules.Module, Long> flash = new HashMap<>();

    // окно выбора
    private Modules.Module popup = null;
    private float pScroll = 0f;
    private long popupMs = 0L;

    // касание / перетаскивание: клик срабатывает при отпускании, если палец не тянул
    private int pressMode = 0;                    // 0 нет, 1 список, 2 окно выбора
    private boolean dragged = false;
    private double pressX, pressY;
    private float scrollAtPress;
    private int pressBtn;

    public MenuScreen() {
        super(Text.literal("NeonComfort"));
    }

    // ================================================================= раскладка

    private boolean simple() { return Modules.MENU_SIMPLE.enabled; }
    private int tabCount() { return 1 + Modules.PAGES.size(); }
    private String tabName(int k) { return k == 0 ? "Quick" : (k - 1 < PAGE_NAMES.length ? PAGE_NAMES[k - 1] : "Page " + k); }

    private List<Modules.Module> list() {
        if (simple()) return Modules.SIMPLE;
        if (tab <= 0) return Modules.QUICK;
        return Modules.PAGES.get(Math.min(tab - 1, Modules.PAGES.size() - 1));
    }

    private int pw() { return Math.min(width - 8, 480); }
    private int ph() { return Math.min(height - 8, 320); }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private boolean wide() { return !simple() && pw() >= 300; }
    private boolean strip() { return !simple() && !wide(); }
    private int sideW() { return wide() ? Math.min(98, pw() / 4) : 0; }
    private int cx0() { return px() + sideW() + 7; }
    private int cx1() { return px() + pw() - 7; }
    private int cy0() { return py() + HEAD + (strip() ? STRIP + 4 : 0) + 3; }
    private int cy1() { return py() + ph() - FOOT - 2; }

    private float maxScroll() {
        return Math.max(0, list().size() * (ROW + GAP) - GAP - (cy1() - cy0()));
    }

    private int tabH() {
        int avail = ph() - HEAD - FOOT - 8;
        return Math.max(15, Math.min(23, avail / tabCount()));
    }
    private int tabY(int k) { return py() + HEAD + 5 + k * tabH(); }

    /** кнопки в шапке: 0 Menu (Simple/Full), 1 язык, 2 Lite, 3 закрыть. Возвращает {x, y, w, h}. */
    private int[] chip(int k) {
        String[] lab = chipLabels();
        int[] w = new int[lab.length];
        int total = 0;
        for (int i = 0; i < lab.length; i++) {
            w[i] = textRenderer.getWidth(lab[i]) + 12;
            total += w[i] + (i > 0 ? 3 : 0);
        }
        int x = px() + pw() - 7 - total;
        for (int i = 0; i < k; i++) x += w[i] + 3;
        return new int[] { x, py() + (HEAD - CHIP_H) / 2 + 1, w[k], CHIP_H };
    }

    private String[] chipLabels() {
        return new String[] {
            Lang.t(simple() ? "Menu: Simple" : "Menu: Full"),
            Lang.buttonLabel(),
            Lang.t(Modules.LITE.enabled ? "Lite: ON" : "Lite: OFF"),
            "X"
        };
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------- окно выбора ----------

    private static int colorOf(String label) {
        for (int i = 0; i < Theme.colorCount() && i < Theme.COLOR_NAMES.length; i++) {
            if (Theme.COLOR_NAMES[i].equals(label)) return Theme.colorRgb(i) & 0xFFFFFF;
        }
        return -1;
    }

    private static boolean isPalette(Modules.Module m) {
        if (m == null || !m.isOption()) return false;
        int c = 0;
        for (String v : m.values) if (colorOf(v) >= 0) c++;
        return c >= 8;
    }

    private int popW() { return Math.min(pw() - 18, isPalette(popup) ? 280 : 250); }

    private int popContentH() {
        if (popup == null) return 0;
        int bw = popW() - 16;
        if (isPalette(popup)) {
            int y = 0, x = 0, colors = 0;
            boolean anySpecial = false;
            for (String v : popup.values) {
                if (colorOf(v) >= 0) { colors++; continue; }
                anySpecial = true;
                int w = textRenderer.getWidth(Lang.t(v)) + 16;
                if (x > 0 && x + w > bw) { x = 0; y += 22; }
                x += w + 4;
            }
            if (anySpecial) y += 26;
            int cols = Math.max(1, (bw + TGAP) / (TILE + TGAP));
            int rows = (colors + cols - 1) / cols;
            return y + rows * (TILE + TGAP);
        }
        return popup.values.length * (POP_ROW + 2);
    }

    private int popH() { return Math.min(ph() - 8, 30 + popContentH() + 8); }
    private int popX() { return (width - popW()) / 2; }
    private int popY() { return (height - popH()) / 2; }
    private int popViewY() { return popY() + 30; }
    private int popViewH() { return popH() - 30 - 8; }
    private float popMaxScroll() { return Math.max(0, popContentH() - popViewH()); }

    /** ячейки окна выбора: {x, y, w, h, индекс значения} (с учётом прокрутки) */
    private List<int[]> popCells() {
        List<int[]> out = new ArrayList<>();
        if (popup == null) return out;
        int bx = popX() + 8, by = popViewY() - (int) pScroll, bw = popW() - 16;
        if (isPalette(popup)) {
            int y = 0, x = 0;
            boolean anySpecial = false;
            for (int i = 0; i < popup.values.length; i++) {
                if (colorOf(popup.values[i]) >= 0) continue;
                anySpecial = true;
                int w = textRenderer.getWidth(Lang.t(popup.values[i])) + 16;
                if (x > 0 && x + w > bw) { x = 0; y += 22; }
                out.add(new int[] { bx + x, by + y, w, 18, i });
                x += w + 4;
            }
            if (anySpecial) y += 26;
            int cols = Math.max(1, (bw + TGAP) / (TILE + TGAP)), n = 0;
            for (int i = 0; i < popup.values.length; i++) {
                if (colorOf(popup.values[i]) < 0) continue;
                out.add(new int[] { bx + (n % cols) * (TILE + TGAP), by + y + (n / cols) * (TILE + TGAP), TILE, TILE, i });
                n++;
            }
        } else {
            for (int i = 0; i < popup.values.length; i++) out.add(new int[] { bx, by + i * (POP_ROW + 2), bw, POP_ROW, i });
        }
        return out;
    }

    // ================================================================= рисование

    private static void rr(DrawContext c, int x, int y, int w, int h, int col) {
        if (w < 5 || h < 5) {
            c.fill(x, y, x + w, y + h, col);
            return;
        }
        c.fill(x + 2, y, x + w - 2, y + 1, col);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, col);
        c.fill(x, y + 2, x + w, y + h - 2, col);
        c.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, col);
        c.fill(x + 2, y + h - 1, x + w - 2, y + h, col);
    }

    private static float ease(float p) {
        float q = 1f - Math.max(0f, Math.min(1f, p));
        return 1f - q * q * q;
    }

    private float openEase() {
        int a = Modules.MENU_ANIM.index;
        if (a == 0) return 1f;
        return ease((System.currentTimeMillis() - openMs) / (a == 1 ? 160f : 320f));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, ((int) (0xB0 * openEase())) << 24);
    }

    private static int swatch(Modules.Module m) {
        if (!m.isOption()) return -1;
        String l = m.label();
        int c = colorOf(l);
        if (c >= 0) return c;
        if (l.equals("Rainbow")) return Theme.rainbowRgb(0L) & 0xFFFFFF;
        if (isPalette(m) && (l.equals("Menu") || l.equals("Aura") || l.equals("Auto"))) return Theme.accent() & 0xFFFFFF;
        return -1;
    }

    private static int a(float alpha, int rgb) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    private void chevron(DrawContext c, int cx, int cy, int col) {
        c.fill(cx - 2, cy - 1, cx + 3, cy, col);
        c.fill(cx - 1, cy, cx + 2, cy + 1, col);
        c.fill(cx, cy + 1, cx + 1, cy + 2, col);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        long nowMs = System.currentTimeMillis();
        long nowNs = System.nanoTime();
        float dt = lastNs == 0L ? 0f : Math.min(0.1f, (nowNs - lastNs) / 1.0e9f);
        lastNs = nowNs;
        scroll = Math.max(0f, Math.min(maxScroll(), scroll));
        if (popup != null) pScroll = Math.max(0f, Math.min(popMaxScroll(), pScroll));
        int anim = Modules.MENU_ANIM.index;
        float e = openEase();
        boolean pop = popup != null;
        int mx = pop ? -1000 : mouseX, my = pop ? -1000 : mouseY;     // под окном выбора подсветки нет

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        if (anim > 0 && e < 1f) ms.translate(0f, (1f - e) * 16f, 0f);

        int x = px(), y = py(), w = pw(), h = ph();
        int acc = Theme.accent() & 0xFFFFFF;
        int acc2 = Theme.accent2() & 0xFFFFFF;

        // --- корпус ---
        rr(ctx, x - 4, y - 4, w + 8, h + 8, 0x30000000);
        rr(ctx, x - 1, y - 1, w + 2, h + 2, a(0.85f, acc));
        rr(ctx, x, y, w, h, Theme.panel());
        ctx.fillGradient(x + 2, y + 1, x + w - 2, y + HEAD, a(0.30f, acc), a(0f, acc));
        ctx.fill(x + 8, y + HEAD, x + w - 8, y + HEAD + 1, 0x22FFFFFF);

        // --- шапка ---
        int[] c0 = chip(0);
        String ver = "v" + NeonComfort.VERSION;
        int tx = x + 10;
        boolean room = tx + textRenderer.getWidth("NeonComfort " + ver) + 8 < c0[0];
        ctx.drawText(textRenderer, "Neon", tx, y + 10, 0xFF000000 | acc, true);
        ctx.drawText(textRenderer, "Comfort", tx + textRenderer.getWidth("Neon"), y + 10, Theme.TEXT, true);
        if (room) ctx.drawText(textRenderer, ver, tx + textRenderer.getWidth("NeonComfort") + 4, y + 10, Theme.TEXT_DIM, true);
        String[] labs = chipLabels();
        for (int k = 0; k < labs.length; k++) {
            int[] c = chip(k);
            boolean hv = in(mx, my, c[0], c[1], c[2], c[3]);
            boolean act = k == 2 && Modules.LITE.enabled;
            rr(ctx, c[0], c[1], c[2], c[3], act ? 0xFF1FA350 : (hv ? a(1f, acc) : 0xFF2A2A3D));
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(labs[k]), c[0] + c[2] / 2, c[1] + 4, Theme.TEXT);
        }

        // --- вкладки ---
        if (wide()) {
            int sw = sideW();
            rr(ctx, x + 4, y + HEAD + 3, sw - 2, h - HEAD - FOOT - 5, 0x44000000);
            for (int k = 0; k < tabCount(); k++) {
                int ty = tabY(k), th = tabH() - 2;
                boolean sel = k == tab, hv = in(mx, my, x + 6, ty, sw - 6, th);
                if (sel) {
                    rr(ctx, x + 7, ty, sw - 8, th, a(1f, acc));
                    ctx.fillGradient(x + 8, ty + 1, x + sw - 2, ty + th / 2, 0x33FFFFFF, 0x00FFFFFF);
                    ctx.fill(x + 7, ty + 3, x + 9, ty + th - 3, 0xFFFFFFFF);
                } else if (hv) rr(ctx, x + 7, ty, sw - 8, th, 0x44FFFFFF);
                String nm = Lang.t(tabName(k));
                ctx.drawText(textRenderer, textRenderer.trimToWidth(nm, sw - 24), x + 14, ty + (th - 8) / 2, sel ? Theme.TEXT : Theme.TEXT_DIM, true);
            }
        } else if (strip()) {
            int sy = y + HEAD + 4, sx = x + 7, sw2 = w - 14;
            rr(ctx, sx, sy, sw2, STRIP, 0x44000000);
            boolean lh = in(mx, my, sx, sy, 24, STRIP), rh = in(mx, my, sx + sw2 - 24, sy, 24, STRIP);
            rr(ctx, sx, sy, 24, STRIP, lh ? a(1f, acc) : 0xFF2A2A3D);
            rr(ctx, sx + sw2 - 24, sy, 24, STRIP, rh ? a(1f, acc) : 0xFF2A2A3D);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("<"), sx + 12, sy + 6, Theme.TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(">"), sx + sw2 - 12, sy + 6, Theme.TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal((tab + 1) + "/" + tabCount() + "  " + Lang.t(tabName(tab))),
                    sx + sw2 / 2, sy + 6, Theme.TEXT);
        }

        // --- карточки ---
        List<Modules.Module> list = list();
        int lx0 = cx0(), lx1 = cx1(), ly0 = cy0(), ly1 = cy1(), rw = lx1 - lx0 - (maxScroll() > 0 ? 6 : 0);
        ctx.enableScissor(lx0, ly0, lx1, ly1);
        for (int i = 0; i < list.size(); i++) {
            int ry = ly0 - (int) scroll + i * (ROW + GAP);
            if (ry + ROW < ly0 || ry > ly1) continue;
            Modules.Module m = list.get(i);
            boolean on = m.isOn();
            float target = on ? 1f : 0f;
            float t = glow.getOrDefault(m, target);
            if (anim == 0) t = target;
            else {
                float step = dt * (anim == 1 ? 18f : 9f);
                t = t < target ? Math.min(target, t + step) : Math.max(target, t - step);
            }
            glow.put(m, t);
            int visTop = Math.max(ry, ly0), visBot = Math.min(ry + ROW, ly1);
            boolean hover = !dragged && in(mx, my, lx0, visTop, rw, Math.max(0, visBot - visTop));
            long fl = flash.getOrDefault(m, 0L);
            float fk = Math.max(0f, 1f - (nowMs - fl) / 220f);

            int base = hover ? 0x363650 : 0x252538;
            int onC = FxColor.lerp(base, acc2, 0.50f);
            int bg = FxColor.lerp(base, onC, t);
            bg = FxColor.lerp(bg, 0xFFFFFF, 0.18f * fk);
            rr(ctx, lx0, ry, rw, ROW, 0xFF000000 | bg);
            ctx.fillGradient(lx0 + 2, ry + 1, lx0 + rw - 2, ry + ROW / 2, 0x16FFFFFF, 0x00FFFFFF);       // глянец сверху
            if (t > 0.02f) rr(ctx, lx0, ry + 4, 3, ROW - 8, a(t, acc));                                     // метка «включено»

            int ty = ry + (ROW - 8) / 2;
            int right = lx0 + rw - 8;
            int valueLeft;
            if (!m.isOption()) {                                                                             // тумблер
                int sx = right - 24, sy = ry + (ROW - 12) / 2;
                rr(ctx, sx, sy, 24, 12, 0xFF000000 | FxColor.lerp(0x4A4A60, acc, t));
                int kx = sx + 2 + (int) (t * 12f);
                rr(ctx, kx, sy + 2, 8, 8, 0xFFFFFFFF);
                valueLeft = sx;
            } else {                                                                                         // «таблетка» со значением
                String st = Lang.t(m.label());
                boolean multi = m.values.length > 2;
                int sc = swatch(m);
                int tw = textRenderer.getWidth(st);
                int pillW = tw + 12 + (sc >= 0 ? 12 : 0) + (multi ? 11 : 0);
                int pillX = right - pillW;
                boolean pillHover = hover && mx >= pillX - 4;
                rr(ctx, pillX, ry + 4, pillW, ROW - 8, pillHover ? 0x66000000 : 0x44000000);
                int cx = pillX + 6;
                if (sc >= 0) {
                    rr(ctx, cx, ry + (ROW - 8) / 2, 8, 8, 0xFF000000 | sc);
                    cx += 12;
                }
                int vc = 0xFF000000 | FxColor.lerp(0xA9A9C4, 0xFFFFFF, Math.max(t, hover ? 1f : 0f));
                if (!pop) ctx.drawText(textRenderer, st, cx, ty, vc, true);
                if (multi) chevron(ctx, pillX + pillW - 7, ry + ROW / 2 - 1, 0xFFA9A9C4);
                valueLeft = pillX;
            }
            if (!pop) ctx.drawText(textRenderer, textRenderer.trimToWidth(Lang.t(m.name), Math.max(10, valueLeft - lx0 - 16)),
                    lx0 + 9, ty, Theme.TEXT, true);
        }
        ctx.disableScissor();

        float mxs = maxScroll();
        if (mxs > 0) {
            int trackH = ly1 - ly0, barH = Math.max(14, (int) (trackH * (trackH / (trackH + mxs))));
            int by = ly0 + (int) ((trackH - barH) * (scroll / mxs));
            rr(ctx, lx1 - 3, ly0, 3, trackH, 0x22FFFFFF);
            rr(ctx, lx1 - 3, by, 3, barH, a(1f, acc));
        }

        if (!pop) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.t("Tap: choose | drag: scroll | right click: back")),
                x + w / 2, y + h - FOOT + 3, Theme.TEXT_DIM);

        if (pop) {
            ctx.draw();   // выгрузить текст меню ДО окна выбора, иначе он рисуется поверх него
            renderPopup(ctx, mouseX, mouseY, nowMs, acc);
        }
        ms.pop();
    }

    private void renderPopup(DrawContext ctx, int mouseX, int mouseY, long nowMs, int acc) {
        float pe = ease((nowMs - popupMs) / 160f);
        ctx.fill(0, 0, width, height, ((int) (0x99 * pe)) << 24);
        int x = popX(), y = popY() + (int) ((1f - pe) * 8f), w = popW(), h = popH();
        rr(ctx, x - 4, y - 4, w + 8, h + 8, 0x40000000);
        rr(ctx, x - 1, y - 1, w + 2, h + 2, a(0.9f, acc));
        rr(ctx, x, y, w, h, 0xFF000000 | (Theme.panel() & 0xFFFFFF));
        ctx.fillGradient(x + 2, y + 1, x + w - 2, y + 28, a(0.30f, acc), a(0f, acc));
        ctx.drawText(textRenderer, textRenderer.trimToWidth(Lang.t(popup.name), w - 60), x + 10, y + 10, Theme.TEXT, true);
        boolean xh = in(mouseX, mouseY, x + w - 24, y + 5, 18, 18);
        rr(ctx, x + w - 24, y + 5, 18, 18, xh ? a(1f, acc) : 0xFF2A2A3D);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("X"), x + w - 15, y + 10, Theme.TEXT);

        int vy = popViewY(), vh = popViewH();
        int caption = -1;
        List<int[]> cells = popCells();
        ctx.enableScissor(x + 4, vy, x + w - 4, vy + vh);
        boolean pal = isPalette(popup);
        for (int[] c : cells) {
            if (c[1] + c[3] < vy || c[1] > vy + vh) continue;
            int idx = c[4];
            String lab = popup.values[idx];
            boolean sel = idx == popup.index;
            boolean hv = in(mouseX, mouseY, c[0], Math.max(c[1], vy), c[2], c[3]) && mouseY >= vy && mouseY < vy + vh && !dragged;
            if (hv) caption = idx;
            int col = colorOf(lab);
            if (pal && col >= 0) {                                                       // квадрат палитры
                if (sel) rr(ctx, c[0] - 2, c[1] - 2, c[2] + 4, c[3] + 4, 0xFFFFFFFF);
                else if (hv) rr(ctx, c[0] - 2, c[1] - 2, c[2] + 4, c[3] + 4, a(1f, acc));
                rr(ctx, c[0], c[1], c[2], c[3], 0xFF000000 | col);
                ctx.fillGradient(c[0] + 1, c[1] + 1, c[0] + c[2] - 1, c[1] + c[3] / 2, 0x33FFFFFF, 0x00FFFFFF);
                if (sel) {
                    int cx = c[0] + c[2] / 2, cy = c[1] + c[3] / 2;
                    ctx.fill(cx - 3, cy, cx - 1, cy + 2, 0xFF101018);                        // галочка
                    ctx.fill(cx - 1, cy + 1, cx + 1, cy + 3, 0xFF101018);
                    ctx.fill(cx + 1, cy - 3, cx + 3, cy + 2, 0xFF101018);
                }
            } else if (pal) {                                                            // особые значения: Auto / Menu / OFF / Rainbow
                boolean rainbow = lab.equals("Rainbow");
                boolean accentLike = lab.equals("Menu") || lab.equals("Aura") || lab.equals("Auto") || lab.equals("Original");
                if (sel) rr(ctx, c[0] - 2, c[1] - 2, c[2] + 4, c[3] + 4, 0xFFFFFFFF);
                else if (hv) rr(ctx, c[0] - 2, c[1] - 2, c[2] + 4, c[3] + 4, a(1f, acc));
                if (rainbow) {
                    int seg = 7, sw = Math.max(1, c[2] / seg);
                    for (int s = 0; s < seg; s++) {
                        int cc = Theme.rainbowRgb(s * 400L) & 0xFFFFFF;
                        int xs = c[0] + s * sw, xe = s == seg - 1 ? c[0] + c[2] : xs + sw;
                        ctx.fill(xs, c[1], xe, c[1] + c[3], 0xFF000000 | cc);
                    }
                } else rr(ctx, c[0], c[1], c[2], c[3], accentLike ? a(1f, acc) : 0xFF2A2A3D);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.t(lab)), c[0] + c[2] / 2, c[1] + 5, Theme.TEXT);
            } else {                                                                     // обычный список значений
                rr(ctx, c[0], c[1], c[2], c[3], sel ? a(1f, acc) : (hv ? 0xFF3A3A55 : 0xFF2A2A3D));
                if (sel) ctx.fillGradient(c[0] + 2, c[1] + 1, c[0] + c[2] - 2, c[1] + c[3] / 2, 0x33FFFFFF, 0x00FFFFFF);
                rr(ctx, c[0] + 6, c[1] + 6, 6, 6, sel ? 0xFFFFFFFF : 0xFF55556B);        // «радио-точка»
                ctx.drawText(textRenderer, textRenderer.trimToWidth(Lang.t(lab), c[2] - 26), c[0] + 18, c[1] + 5, Theme.TEXT, true);
                int sc = colorOf(lab);
                if (sc >= 0) rr(ctx, c[0] + c[2] - 14, c[1] + 5, 8, 8, 0xFF000000 | sc);
            }
        }
        ctx.disableScissor();

        if (pal) {                                                                       // подпись выбранного/наведённого цвета
            int idx = caption >= 0 ? caption : popup.index;
            String cap = Lang.t(popup.values[Math.max(0, Math.min(popup.values.length - 1, idx))]);
            ctx.drawText(textRenderer, cap, x + w - 30 - textRenderer.getWidth(cap), y + 10, Theme.TEXT_DIM, true);
        }
        float pm = popMaxScroll();
        if (pm > 0) {
            int barH = Math.max(12, (int) (vh * (vh / (vh + pm))));
            int by = vy + (int) ((vh - barH) * (pScroll / pm));
            rr(ctx, x + w - 6, vy, 3, vh, 0x22FFFFFF);
            rr(ctx, x + w - 6, by, 3, barH, a(1f, acc));
        }
    }

    // ================================================================= ввод

    private void setTab(int k) {
        k = (k % tabCount() + tabCount()) % tabCount();
        if (k != tab) {
            tab = k;
            lastTab = k;
            scroll = 0f;
            UiSound.play(UiSound.TAB);
        }
    }

    private void openPopup(Modules.Module m) {
        popup = m;
        popupMs = System.currentTimeMillis();
        int rows = 0;
        for (int i = 0; i < m.values.length; i++) if (i < m.index) rows++;
        pScroll = isPalette(m) ? 0f : Math.max(0f, (m.index - 2) * (POP_ROW + 2));
        UiSound.play(UiSound.OPEN);
    }

    private void closePopup() {
        popup = null;
        pressMode = 0;
        UiSound.play(UiSound.CLOSE);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && popup != null) {            // Esc закрывает окно выбора, а не всё меню
            closePopup();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            int x = popX(), y = popY(), w = popW(), h = popH();
            if (!in(mx, my, x, y, w, h) || button == 1 || in(mx, my, x + w - 24, y + 5, 18, 18)) {
                closePopup();
                return true;
            }
            if (in(mx, my, x + 4, popViewY(), w - 8, popViewH())) {
                pressMode = 2;
                dragged = false;
                pressX = mx;
                pressY = my;
                scrollAtPress = pScroll;
                pressBtn = button;
            }
            return true;
        }
        for (int k = 0; k < 4; k++) {
            int[] c = chip(k);
            if (!in(mx, my, c[0], c[1], c[2], c[3])) continue;
            UiSound.play(k == 3 ? UiSound.CLOSE : UiSound.OPEN);
            switch (k) {
                case 0 -> {
                    Modules.MENU_SIMPLE.enabled = !Modules.MENU_SIMPLE.enabled;
                    scroll = 0f;
                    Config.save();
                }
                case 1 -> {
                    Modules.LANG.index = (Modules.LANG.index + 1) % Modules.LANG.values.length;
                    Config.save();
                }
                case 2 -> Lite.setEnabled(!Modules.LITE.enabled);
                default -> close();
            }
            return true;
        }
        if (wide()) {
            for (int k = 0; k < tabCount(); k++) {
                if (in(mx, my, px() + 6, tabY(k), sideW() - 6, tabH() - 2)) {
                    setTab(k);
                    return true;
                }
            }
        } else if (strip()) {
            int sy = py() + HEAD + 4, sx = px() + 7, sw2 = pw() - 14;
            if (in(mx, my, sx, sy, 24, STRIP)) { setTab(tab - 1); return true; }
            if (in(mx, my, sx + sw2 - 24, sy, 24, STRIP)) { setTab(tab + 1); return true; }
        }
        if (in(mx, my, cx0(), cy0(), cx1() - cx0(), cy1() - cy0())) {
            pressMode = 1;
            dragged = false;
            pressX = mx;
            pressY = my;
            scrollAtPress = scroll;
            pressBtn = button;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressMode != 0 && button == 0) {
            if (dragged || Math.abs(my - pressY) > 4) {
                dragged = true;
                if (pressMode == 2) pScroll = Math.max(0f, Math.min(popMaxScroll(), scrollAtPress - (float) (my - pressY)));
                else scroll = Math.max(0f, Math.min(maxScroll(), scrollAtPress - (float) (my - pressY)));
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressMode != 0) {
            int mode = pressMode;
            pressMode = 0;
            boolean was = dragged;
            dragged = false;
            if (!was) {
                if (mode == 2) popupClick(pressX, pressY);
                else clickRow(pressX, pressY, pressBtn);
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (popup != null) pScroll = Math.max(0f, Math.min(popMaxScroll(), pScroll - (float) vertical * 24f));
        else scroll = Math.max(0f, Math.min(maxScroll(), scroll - (float) vertical * 24f));
        return true;
    }

    private void popupClick(double mx, double my) {
        if (popup == null) return;
        if (my < popViewY() || my >= popViewY() + popViewH()) return;
        for (int[] c : popCells()) {
            if (!in(mx, my, c[0], c[1], c[2], c[3])) continue;
            Modules.Module m = popup;
            m.index = c[4];
            flash.put(m, System.currentTimeMillis());
            popup = null;
            UiSound.play(UiSound.PICK);
            activate(m);
            return;
        }
    }

    /** клик по карточке: переключатель меняется сразу, пункт с выбором открывает окно, ПКМ = предыдущее значение */
    private void clickRow(double mx, double my, int button) {
        List<Modules.Module> list = list();
        int lx0 = cx0(), rw = cx1() - lx0 - (maxScroll() > 0 ? 6 : 0);
        for (int i = 0; i < list.size(); i++) {
            int ry = cy0() - (int) scroll + i * (ROW + GAP);
            if (!in(mx, my, lx0, ry, rw, ROW)) continue;
            Modules.Module m = list.get(i);
            if (m.isOption() && m.values.length > 2 && button != 1) {
                openPopup(m);
                return;
            }
            if (button == 1) m.back(); else m.click();
            flash.put(m, System.currentTimeMillis());
            UiSound.play(m.isOn() ? UiSound.PICK : UiSound.OFF);
            activate(m);
            return;
        }
    }

    /** побочные действия кнопок (пресеты, точки, команда, звук...) */
    private void activate(Modules.Module m) {
        MinecraftClient cmc = MinecraftClient.getInstance();
        if (m == Modules.PRESET_APPLY) Presets.apply(Modules.PRESET.index);
        if (m == Modules.PRESET_UNDO) Presets.undo();
        if (m == Modules.LOOK_APPLY) Presets.applyLook(Modules.LOOK.index);
        if (m == Modules.TEAM_ADD) Nav.addNearest(cmc);
        if (m == Modules.TEAM_CLEAR) Nav.clearTeam();
        if (m == Modules.WP_ADD) Nav.addHere(cmc);
        if (m == Modules.WP_CLEAR) Nav.clearWaypoints();
        if (m == Modules.WP_SHARE) Nav.share(cmc);
        Config.save();
        if (m == Modules.HIT_SOUND) NeonComfort.playHitSound();
        if (m == Modules.FPS_BOOST) Performance.apply();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
