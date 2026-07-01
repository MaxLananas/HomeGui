package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.ui.UIRenderer;
import com.maxlananas.homegui.ui.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HistoryScreen extends Screen {

    private final Screen parent;
    private int hoveredIndex = -1;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW;

    public HistoryScreen(Screen parent) {
        super(Component.literal("History"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(UITheme.PANEL_W - 20, width - 40);
        panelH = Math.min(height - 40, 300);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;
        listX  = panelX + UITheme.PAD;
        listY  = panelY + UITheme.HEADER_H + 6;
        listW  = panelW - UITheme.PAD * 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        UIRenderer.drawBackground(g, width, height);
        UIRenderer.drawPanel(g, panelX, panelY, panelW, panelH);

        // Header
        UIRenderer.drawHeader(g, panelX, panelY, panelW, UITheme.HEADER_H);
        UIRenderer.drawTitle(g, font,
                "⟳  " + LangManager.getInstance().get("title.history"),
                panelX + panelW / 2, panelY + 8, UITheme.ACCENT_TITLE);

        // Liste
        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        hoveredIndex = -1;

        if (history.isEmpty()) {
            String msg = LangManager.getInstance().get("message.no_history");
            g.drawString(font, Component.literal(msg),
                    panelX + panelW / 2 - font.width(msg) / 2,
                    panelY + panelH / 2 - 4,
                    UITheme.TEXT_DIM, false);
        } else {
            int max = Math.min(history.size(), 10);
            for (int i = 0; i < max; i++) {
                ModConfig.HistoryEntry entry = history.get(i);
                int rowY = listY + i * (UITheme.ROW_H + UITheme.ROW_GAP);

                boolean hovered = mouseX >= listX && mouseX <= listX + listW
                               && mouseY >= rowY  && mouseY <= rowY + UITheme.ROW_H;
                if (hovered) hoveredIndex = i;

                UIRenderer.drawRow(g, listX, rowY, listW, UITheme.ROW_H,
                        hovered, false);

                // Numéro
                String num = (i + 1) + ".";
                g.drawString(font, Component.literal(num),
                        listX + 4, rowY + 7,
                        UITheme.TEXT_DIM, false);

                // Nom du home
                g.drawString(font,
                        Component.literal(entry.homeName),
                        listX + 20, rowY + 7,
                        hovered ? UITheme.TEXT_PRIMARY : 0xFFCCCCEE, false);

                // Temps écoulé (aligné à droite)
                String ago = entry.getTimeAgo();
                g.drawString(font, Component.literal(ago),
                        listX + listW - font.width(ago) - 6,
                        rowY + 7,
                        UITheme.TEXT_DIM, false);
            }
        }

        // Footer
        renderFooter(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
        int footerY = panelY + panelH - UITheme.FOOTER_H;
        UIRenderer.drawFooter(g, panelX, footerY, panelW, UITheme.FOOTER_H);

        LangManager lang  = LangManager.getInstance();
        String[] labels   = { lang.get("button.clear"), lang.get("button.back") };
        int btnW          = 70;
        int totalW        = btnW * 2 + 8;
        int startX        = panelX + (panelW - totalW) / 2;
        int btnY          = footerY + (UITheme.FOOTER_H - 14) / 2;

        for (int i = 0; i < 2; i++) {
            int bx     = startX + i * (btnW + 8);
            boolean bh = mouseX >= bx && mouseX <= bx + btnW
                      && mouseY >= btnY && mouseY <= btnY + 14;

            g.fill(bx, btnY, bx + btnW, btnY + 14,
                    bh ? UITheme.BTN_BG_HOVER : UITheme.BTN_BG);
            UIRenderer.drawBorder(g, bx, btnY, btnW, 14,
                    bh ? UITheme.ACCENT_PRIMARY : UITheme.BTN_BORDER);

            String lbl = labels[i];
            g.drawString(font, Component.literal(lbl),
                    bx + btnW / 2 - font.width(lbl) / 2, btnY + 3,
                    bh ? UITheme.ACCENT_TITLE : UITheme.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int mouseX = (int) mx;
        int mouseY = (int) my;

        // Clic sur un historique
        if (hoveredIndex >= 0 && btn == 0) {
            List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
            if (hoveredIndex < history.size()) {
                String home = history.get(hoveredIndex).homeName;
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }
        }

        // Footer
        int footerY = panelY + panelH - UITheme.FOOTER_H;
        int btnW    = 70;
        int startX  = panelX + (panelW - (btnW * 2 + 8)) / 2;
        int btnY    = footerY + (UITheme.FOOTER_H - 14) / 2;

        for (int i = 0; i < 2; i++) {
            int bx = startX + i * (btnW + 8);
            if (mouseX >= bx && mouseX <= bx + btnW
             && mouseY >= btnY && mouseY <= btnY + 14) {
                if (i == 0) {
                    ModConfig.getInstance().clearHistory();
                } else {
                    if (minecraft != null) minecraft.setScreen(parent);
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
