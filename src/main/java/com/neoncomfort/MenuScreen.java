package com.neoncomfort;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class MenuScreen extends Screen {
    private static final int BTN_W = 140, BTN_H = 22, GAP = 5;

    private int page = 0;

    public MenuScreen() {
        super(Text.literal("NeonComfort"));
    }

    private int cols() {
        return Math.max(2, Math.min(3, (width - 20) / (BTN_W + GAP)));
    }
    private int panelW() { return BTN_W * cols() + GAP * (cols() + 1); }
    private int panelH() {
        int rows = (Modules.PAGE1.size() + cols() - 1) / cols();
        return 40 + rows * (BTN_H + GAP) + GAP;
    }
    private int px() { return (width - panelW()) / 2; }
    private int py() { return (height - panelH()) / 2; }

    private static final int PB_W = 56, PB_H = 14;
    private int pageBtnX() { return px() + panelW() - PB_W - 6; }
    private int pageBtnY() { return py() + 6; }

    private int btnX(int i) { return px() + GAP + (i % cols()) * (BTN_W + GAP); }
    private int btnY(int i) { return py() + 40 + (i / cols()) * (BTN_H + GAP); }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // без размытия: лёгкое затемнение
        ctx.fill(0, 0, width, height, 0x99000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int x = px(), y = py(), w = panelW(), h = panelH();
        int border = Theme.pulsingAccent();

        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        ctx.fill(x, y, x + w, y + h, Theme.PANEL);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NeonComfort v" + NeonComfort.VERSION), width / 2, y + 8, Theme.TEXT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("tap to toggle / change"),
                width / 2, y + 22, Theme.TEXT_DIM);

        // кнопка страницы (справа вверху)
        int pbx = pageBtnX(), pby = pageBtnY();
        boolean ph = mouseX >= pbx && mouseX < pbx + PB_W && mouseY >= pby && mouseY < pby + PB_H;
        ctx.fill(pbx, pby, pbx + PB_W, pby + PB_H, ph ? Theme.accent() : Theme.accent2());
        String pl = "Page " + (page + 1) + "/" + Modules.PAGES.size();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pl), pbx + PB_W / 2, pby + 3, Theme.TEXT);

        List<Modules.Module> list = Modules.PAGES.get(page);
        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            int bx = btnX(i), by = btnY(i);
            boolean on = m.isOn();
            boolean hover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
            int base = on ? (hover ? Theme.accent() : Theme.accent2()) : (hover ? 0xFF3A3A55 : Theme.OFF);
            ctx.fill(bx, by, bx + BTN_W, by + BTN_H, base);
            if (on) {
                ctx.fill(bx, by, bx + BTN_W, by + 1, Theme.pulsingAccent());
                ctx.fill(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, Theme.pulsingAccent());
            }
            int ty = by + (BTN_H - 8) / 2;
            ctx.drawText(textRenderer, m.name, bx + 8, ty, Theme.TEXT, true);
            String st = m.label();
            ctx.drawText(textRenderer, st, bx + BTN_W - 8 - textRenderer.getWidth(st), ty,
                    on ? 0xFFFFFFFF : Theme.TEXT_DIM, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= pageBtnX() && mx < pageBtnX() + PB_W && my >= pageBtnY() && my < pageBtnY() + PB_H) {
            page = (page + 1) % Modules.PAGES.size();
            return true;
        }
        List<Modules.Module> list = Modules.PAGES.get(page);
        for (int i = 0; i < list.size(); i++) {
            int bx = btnX(i), by = btnY(i);
            if (mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H) {
                list.get(i).click();
                Config.save();
                if (list.get(i) == Modules.HIT_SOUND) NeonComfort.playHitSound();
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
