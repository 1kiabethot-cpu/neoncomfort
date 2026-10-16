package com.neoncomfort;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;

public class MenuScreen extends Screen {
    private static final int BTN_W = 140, BTN_H = 22, GAP = 5;

    private int page = 0;

    // --- анимация меню ---
    private final long openMs = System.currentTimeMillis();
    private long pageMs = openMs;                       // начало «волны» кнопок (при открытии и смене страницы)
    private long lastNs = 0L;
    private final java.util.Map<Modules.Module, Float> glow = new java.util.HashMap<>();   // плавное включение кнопок

    private static float ease(float p) {
        float q = 1f - Math.max(0f, Math.min(1f, p));
        return 1f - q * q * q;
    }

    private float openEase() {
        int a = Modules.MENU_ANIM.index;
        if (a == 0) return 1f;
        float dur = a == 1 ? 160f : 360f;
        return ease((System.currentTimeMillis() - openMs) / dur);
    }

    public MenuScreen() {
        super(Text.literal("NeonComfort"));
    }

    private boolean simple() { return Modules.MENU_SIMPLE.enabled; }

    private List<Modules.Module> cur() {
        return simple() ? Modules.SIMPLE : Modules.PAGES.get(page);
    }

    private int cols() {
        return Math.max(2, Math.min(3, (width - 20) / (BTN_W + GAP)));
    }
    private int panelW() { return BTN_W * cols() + GAP * (cols() + 1); }
    private int panelH() {
        int n = simple() ? Modules.SIMPLE.size() : Modules.PAGE1.size();
        int rows = (n + cols() - 1) / cols();
        return 40 + rows * (BTN_H + GAP) + GAP;
    }
    private int px() { return (width - panelW()) / 2; }
    private int py() { return (height - panelH()) / 2; }

    private static final int PB_W = 56, PB_H = 14;
    private int pageBtnX() { return px() + panelW() - PB_W - 6; }
    private int pageBtnY() { return py() + 22; }

    private static final int MB_W = 84;
    private int modeBtnX() { return px() + 6; }
    private int modeBtnY() { return py() + 22; }

    // кнопка лайт-режима (по центру под заголовком) и кнопка языка (слева вверху)
    private static final int LB_W = 78;
    private int liteBtnX() { return px() + (panelW() - LB_W) / 2; }
    private int liteBtnY() { return py() + 22; }
    private static final int LG_W = 34;
    private int langBtnX() { return px() + 6; }
    private int langBtnY() { return py() + 5; }

    private int btnX(int i) { return px() + GAP + (i % cols()) * (BTN_W + GAP); }
    private int btnY(int i) { return py() + 40 + (i / cols()) * (BTN_H + GAP); }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // без размытия: лёгкое затемнение (плавно проявляется при открытии)
        int a = (int) (0x99 * openEase());
        ctx.fill(0, 0, width, height, a << 24);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int anim = Modules.MENU_ANIM.index;
        float e = openEase();
        long nowNs = System.nanoTime();
        float dt = lastNs == 0L ? 0f : Math.min(0.1f, (nowNs - lastNs) / 1.0e9f);
        lastNs = nowNs;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        if (anim > 0 && e < 1f) {                       // панель выезжает снизу и слегка увеличивается
            float sc = 0.94f + 0.06f * e;
            float cx = width / 2f, cy = height / 2f;
            ms.translate(cx, cy + (1f - e) * 18f, 0f);
            ms.scale(sc, sc, 1f);
            ms.translate(-cx, -cy, 0f);
        }
        int x = px(), y = py(), w = panelW(), h = panelH();
        int border = Theme.pulsingAccent();

        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        ctx.fill(x, y, x + w, y + h, Theme.panel());

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NeonComfort v" + NeonComfort.VERSION), width / 2, y + 8, Theme.TEXT);

        // кнопка языка (слева вверху)
        int lgx = langBtnX(), lgy = langBtnY();
        boolean lgh = mouseX >= lgx && mouseX < lgx + LG_W && mouseY >= lgy && mouseY < lgy + PB_H;
        ctx.fill(lgx, lgy, lgx + LG_W, lgy + PB_H, lgh ? Theme.accent() : Theme.accent2());
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.buttonLabel()), lgx + LG_W / 2, lgy + 3, Theme.TEXT);

        // кнопка лайт-режима (по центру): выключает всё, кроме FPS Boost и Fullbright
        int lbx = liteBtnX(), lby = liteBtnY();
        boolean lbh = mouseX >= lbx && mouseX < lbx + LB_W && mouseY >= lby && mouseY < lby + PB_H;
        boolean lite = Modules.LITE.enabled;
        ctx.fill(lbx, lby, lbx + LB_W, lby + PB_H, lite ? (lbh ? 0xFF2FD36B : 0xFF1FA350) : (lbh ? Theme.accent() : Theme.accent2()));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.t(lite ? "Lite: ON" : "Lite: OFF")),
                lbx + LB_W / 2, lby + 3, Theme.TEXT);

        // кнопка режима меню (слева вверху): Simple / Full
        int mbx = modeBtnX(), mby = modeBtnY();
        boolean mh = mouseX >= mbx && mouseX < mbx + MB_W && mouseY >= mby && mouseY < mby + PB_H;
        ctx.fill(mbx, mby, mbx + MB_W, mby + PB_H, mh ? Theme.accent() : Theme.accent2());
        String ml = Lang.t(simple() ? "Menu: Simple" : "Menu: Full");
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(ml), mbx + MB_W / 2, mby + 3, Theme.TEXT);

        // кнопка страницы (справа вверху), только в полном меню
        if (!simple()) {
            int pbx = pageBtnX(), pby = pageBtnY();
            boolean ph = mouseX >= pbx && mouseX < pbx + PB_W && mouseY >= pby && mouseY < pby + PB_H;
            ctx.fill(pbx, pby, pbx + PB_W, pby + PB_H, ph ? Theme.accent() : Theme.accent2());
            String pl = Lang.t("Page") + " " + (page + 1) + "/" + Modules.PAGES.size();
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pl), pbx + PB_W / 2, pby + 3, Theme.TEXT);
        }

        List<Modules.Module> list = cur();
        long since = System.currentTimeMillis() - pageMs;
        float rowDelay = anim == 1 ? 12f : 28f;
        float rowDur = anim == 1 ? 150f : 260f;
        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            float pr = anim == 0 ? 1f : (since - i * rowDelay) / rowDur;
            if (pr <= 0f) continue;                       // кнопки появляются по очереди, как волна
            float er = ease(pr);
            int bx = btnX(i), by = btnY(i);
            boolean on = m.isOn();
            float target = on ? 1f : 0f;
            float t = glow.getOrDefault(m, target);
            if (anim == 0) {
                t = target;
            } else {
                float step = dt * (anim == 1 ? 18f : 9f);
                t = t < target ? Math.min(target, t + step) : Math.max(target, t - step);
            }
            glow.put(m, t);
            boolean hover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
            ms.push();
            ms.translate((1f - er) * -16f, 0f, 0f);       // выезжает слева
            int offC = hover ? 0x3A3A55 : (Theme.OFF & 0xFFFFFF);
            int onC = (hover ? Theme.accent() : Theme.accent2()) & 0xFFFFFF;
            ctx.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF000000 | FxColor.lerp(offC, onC, t));
            if (t > 0.02f) {
                int bc = ((int) (255 * t) << 24) | (Theme.pulsingAccent() & 0xFFFFFF);
                ctx.fill(bx, by, bx + BTN_W, by + 1, bc);
                ctx.fill(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, bc);
            }
            int ty = by + (BTN_H - 8) / 2;
            ctx.drawText(textRenderer, Lang.t(m.name), bx + 8, ty, Theme.TEXT, true);
            String st = Lang.t(m.label());
            int vc = 0xFF000000 | FxColor.lerp(Theme.TEXT_DIM & 0xFFFFFF, 0xFFFFFF, t);
            ctx.drawText(textRenderer, st, bx + BTN_W - 8 - textRenderer.getWidth(st), ty, vc, true);
            ms.pop();
        }
        ms.pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= langBtnX() && mx < langBtnX() + LG_W && my >= langBtnY() && my < langBtnY() + PB_H) {
            Modules.LANG.index = (Modules.LANG.index + 1) % Modules.LANG.values.length;
            Config.save();
            return true;
        }
        if (mx >= liteBtnX() && mx < liteBtnX() + LB_W && my >= liteBtnY() && my < liteBtnY() + PB_H) {
            Lite.setEnabled(!Modules.LITE.enabled);
            return true;
        }
        if (mx >= modeBtnX() && mx < modeBtnX() + MB_W && my >= modeBtnY() && my < modeBtnY() + PB_H) {
            Modules.MENU_SIMPLE.enabled = !Modules.MENU_SIMPLE.enabled;
            page = 0;
            pageMs = System.currentTimeMillis();
            Config.save();
            return true;
        }
        if (!simple() && mx >= pageBtnX() && mx < pageBtnX() + PB_W && my >= pageBtnY() && my < pageBtnY() + PB_H) {
            page = (page + 1) % Modules.PAGES.size();
            pageMs = System.currentTimeMillis();
            return true;
        }
        List<Modules.Module> list = cur();
        for (int i = 0; i < list.size(); i++) {
            int bx = btnX(i), by = btnY(i);
            if (mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H) {
                list.get(i).click();
                if (list.get(i) == Modules.PRESET) Presets.apply(Modules.PRESET.index);
                Config.save();
                if (list.get(i) == Modules.HIT_SOUND) NeonComfort.playHitSound();
                if (list.get(i) == Modules.FPS_BOOST) Performance.apply();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
