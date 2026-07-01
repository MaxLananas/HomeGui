package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.ui.UIRenderer;
import com.maxlananas.homegui.ui.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class StatsScreen extends Screen {

    private final Screen parent;
    private boolean backHovered = false;

    private int panelX, panelY, panelW, panelH;

    public StatsScreen(Screen parent) {
        super(Component.literal("Statistics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(UITheme.PANEL_W, width - 40);
        panelH = Math.min(height - 40, 340);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        UIRenderer.drawBackground(g, width, height);
        UIRenderer.drawPanel(g, panelX, panelY, panelW, panelH);

        // Header
        UIRenderer.drawHeader(g, panelX, panelY, panelW, UITheme.HEADER_H);
        UIRenderer.drawTitle(g, font,
                "📊  " + LangManager.getInstance().get("title.stats"),
                panelX + panelW / 2, panelY + 8, UITheme.ACCENT_TITLE);

        int y   = panelY + UITheme.HEADER_H + 10;
        int pad = UITheme.PAD;

        // ── Cartes de stats ───────────────────────────────────────────────
        ModConfig cfg     = ModConfig.getInstance();
        LangManager lang  = LangManager.getInstance();
        List<String> homes = HomesManager.getInstance().getHomes();

        long favCount  = homes.stream().filter(cfg::isFavorite).count();
        int  cardW     = (panelW - pad * 2 - 8) / 3;
        int  cardH     = 40;

        renderStatCard(g,
                panelX + pad, y, cardW, cardH,
                String.valueOf(homes.size()),
                lang.get("stats.total_homes"),
                UITheme.ACCENT_PRIMARY);

        renderStatCard(g,
                panelX + pad + cardW + 4, y, cardW, cardH,
                String.valueOf(favCount),
                lang.get("stats.favorites"),
                UITheme.COLOR_GOLD);

        renderStatCard(g,
                panelX + pad + (cardW + 4) * 2, y, cardW, cardH,
                String.valueOf(cfg.getTotalTeleports()),
                lang.get("stats.total_tp"),
                UITheme.COLOR_GREEN);

        y += cardH + 16;

        // ── Séparateur ────────────────────────────────────────────────────
        UIRenderer.drawSeparator(g, panelX + pad, y, panelW - pad * 2);
        String topLabel = lang.get("stats.top_homes");
        g.drawString(font, Component.literal(topLabel),
                panelX + panelW / 2 - font.width(topLabel) / 2,
                y + 4, UITheme.TEXT_DIM, false);
        y += 16;

        // ── Top 5 homes ───────────────────────────────────────────────────
        Map<String, Integer> counts = cfg.getAllUseCounts();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int maxCount = sorted.isEmpty() ? 1 : Math.max(1, sorted.get(0).getValue());
        int barW     = panelW - pad * 2;
        int barH     = 18;
        int[] medals = {UITheme.COLOR_GOLD, UITheme.COLOR_SILVER, UITheme.COLOR_BRONZE};

        if (sorted.isEmpty()) {
            String msg = "Aucune donnée";
            g.drawString(font, Component.literal(msg),
                    panelX + panelW / 2 - font.width(msg) / 2,
                    y + 20, UITheme.TEXT_DIM, false);
        } else {
            int shown = Math.min(5, sorted.size());
            for (int i = 0; i < shown; i++) {
                Map.Entry<String, Integer> entry = sorted.get(i);
                float ratio   = (float) entry.getValue() / maxCount;
                int   barColor = i < 3 ? medals[i] : UITheme.ACCENT_PRIMARY;

                UIRenderer.drawProgressBar(g,
                        panelX + pad, y, barW, barH, ratio, barColor);

                // Médaille / rang
                String rank = i == 0 ? "①" : i == 1 ? "②" : i == 2 ? "③"
                                     : "#" + (i + 1);
                g.drawString(font, Component.literal(rank),
                        panelX + pad + 3, y + 5, barColor, false);

                // Nom
                String name = truncate(entry.getKey(), barW - 80);
                g.drawString(font, Component.literal(name),
                        panelX + pad + 18, y + 5,
                        UITheme.TEXT_PRIMARY, false);

                // Nombre de visites
                int v = entry.getValue();
                String visits = v + " " + (v > 1
                        ? lang.get("stats.visits_plural")
                        : lang.get("stats.visits"));
                g.drawString(font, Component.literal(visits),
                        panelX + pad + barW - font.width(visits) - 4,
                        y + 5, UITheme.TEXT_DIM, false);

                y += barH + 4;
            }
        }

        // Footer / bouton retour
        int footerY = panelY + panelH - UITheme.FOOTER_H;
        UIRenderer.drawFooter(g, panelX, footerY, panelW, UITheme.FOOTER_H);

        int bW   = 80;
        int bX   = panelX + (panelW - bW) / 2;
        int bY   = footerY + (UITheme.FOOTER_H - 14) / 2;
        backHovered = mouseX >= bX && mouseX <= bX + bW
                   && mouseY >= bY && mouseY <= bY + 14;

        g.fill(bX, bY, bX + bW, bY + 14,
                backHovered ? UITheme.BTN_BG_HOVER : UITheme.BTN_BG);
        UIRenderer.drawBorder(g, bX, bY, bW, 14,
                backHovered ? UITheme.ACCENT_PRIMARY : UITheme.BTN_BORDER);

        String backLabel = LangManager.getInstance().get("button.back");
        g.drawString(font, Component.literal(backLabel),
                bX + bW / 2 - font.width(backLabel) / 2, bY + 3,
                backHovered ? UITheme.ACCENT_TITLE : UITheme.TEXT_DIM, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderStatCard(GuiGraphics g, int x, int y, int w, int h,
                                 String value, String label, int color) {
        UIRenderer.drawStatCard(g, x, y, w, h, color);

        // Valeur (grande, colorée)
        UIRenderer.drawTitle(g, font, value, x + w / 2, y + 8, color);

        // Label (petit, gris)
        g.drawString(font, Component.literal(label),
                x + w / 2 - font.width(label) / 2,
                y + 26, UITheme.TEXT_DIM, false);
    }

    private String truncate(String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        while (!text.isEmpty() && font.width(text + "…") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (backHovered && btn == 0) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
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
