package com.neoncomfort;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class MenuScreen extends Screen {
    private static final int BTN_W = 150, BTN_H = 28, GAP = 8;

    public MenuScreen() {
        super(Text.literal("NeonComfort"));
    }

    private int panelW() { return BTN_W * 2 + GAP * 3; }
    private int panelH() {
        int rows = (Modules.ALL.size() + 1) / 2;
        return 44 + rows * (BTN_H + GAP) + GAP;
    }
    private int px() { return (width - panelW()) / 2; }
    private int py() { return (height - panelH()) / 2; }

    private int btnX(int i) { return px() + GAP + (i % 2) * (BTN_W + GAP); }
    private int btnY(int i) { return py() + 44 + (i / 2) * (BTN_H + GAP); }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int x = px(), y = py(), w = panelW(), h = panelH();
        int border = Theme.pulsingAccent();

        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        ctx.fill(x, y, x + w, y + h, Theme.PANEL);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NeonComfort"), width / 2, y + 10, Theme.TEXT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("tap to toggle"),
                width / 2, y + 24, Theme.TEXT_DIM);

        List<Modules.Module> list = Modules.ALL;
        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            int bx = btnX(i), by = btnY(i);
            boolean hover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
            int base = m.enabled ? (hover ? Theme.ACCENT : Theme.ACCENT_2) : (hover ? 0xFF3A3A55 : Theme.OFF);
            ctx.fill(bx, by, bx + BTN_W, by + BTN_H, base);
            if (m.enabled) {
                ctx.fill(bx, by, bx + BTN_W, by + 1, Theme.pulsingAccent());
                ctx.fill(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, Theme.pulsingAccent());
            }
            ctx.drawText(textRenderer, m.name, bx + 10, by + 10, Theme.TEXT, true);
            String st = m.enabled ? "ON" : "OFF";
            ctx.drawText(textRenderer, st, bx + BTN_W - 10 - textRenderer.getWidth(st), by + 10,
                    m.enabled ? 0xFFFFFFFF : Theme.TEXT_DIM, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        List<Modules.Module> list = Modules.ALL;
        for (int i = 0; i < list.size(); i++) {
            int bx = btnX(i), by = btnY(i);
            if (mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H) {
                list.get(i).toggle();
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
