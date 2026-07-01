package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.ui.UIRenderer;
import com.maxlananas.homegui.ui.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.MouseButtonEvent;
import net.minecraft.client.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    // ── Données ───────────────────────────────────────────────────────────
    private final List<String> allHomes = new ArrayList<>();
    private final List<String> filtered = new ArrayList<>();

    // ── UI State ──────────────────────────────────────────────────────────
    private EditBox searchBox;
    private boolean showFavOnly  = false;
    private int     scrollOffset = 0;
    private int     hoveredIndex = -1;
    private long    openTime;

    // ── Layout ────────────────────────────────────────────────────────────
    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int maxVisible;

    public HomesScreen() {
        super(Component.literal("HomeGUI"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();

        panelW = Math.min(UITheme.PANEL_W, width - 40);
        panelH = Math.min(height - 40, 320);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;

        listX = panelX + UITheme.PAD;
        listY = panelY + UITheme.HEADER_H + 26;
        listW = panelW - UITheme.PAD * 2 - UITheme.SCROLLBAR_W - 2;
        listH = panelH - UITheme.HEADER_H - UITheme.FOOTER_H - 30;
        maxVisible = Math.max(1, listH / (UITheme.ROW_H + UITheme.ROW_GAP));

        allHomes.clear();
        allHomes.addAll(HomesManager.getInstance().getHomes());

        // SearchBox
        String prevQuery = (searchBox != null) ? searchBox.getValue() : "";
        int sbY = panelY + UITheme.HEADER_H + 4;
        int sbW = panelW - UITheme.PAD * 2 - 24;

        searchBox = new EditBox(font, listX, sbY, sbW, 16, Component.literal(""));
        searchBox.setValue(prevQuery);
        searchBox.setHint(Component.literal("Rechercher..."));
        searchBox.setBordered(false);
        searchBox.setTextColor(UITheme.TEXT_PRIMARY);
        searchBox.setResponder(t -> {
            scrollOffset = 0;
            applyFilter();
        });
        addRenderableWidget(searchBox);

        applyFilter();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIQUE
    // ─────────────────────────────────────────────────────────────────────

    private void applyFilter() {
        filtered.clear();
        String q = (searchBox != null)
                ? searchBox.getValue().toLowerCase().trim() : "";
        ModConfig cfg = ModConfig.getInstance();

        List<String> favs   = new ArrayList<>();
        List<String> others = new ArrayList<>();

        for (String h : allHomes) {
            boolean matchQ   = q.isEmpty() || h.toLowerCase().contains(q);
            boolean isFav    = cfg.isFavorite(h);
            boolean matchFav = !showFavOnly || isFav;
            if (matchQ && matchFav) {
                if (isFav) favs.add(h);
                else       others.add(h);
            }
        }
        filtered.addAll(favs);
        filtered.addAll(others);

        int maxScroll = Math.max(0, filtered.size() - maxVisible);
        scrollOffset  = Math.min(scrollOffset, maxScroll);
    }

    private void refresh() {
        HomesManager.getInstance().requestHomes();
        allHomes.clear();
        allHomes.addAll(HomesManager.getInstance().getHomes());
        scrollOffset = 0;
        applyFilter();
    }

    // ─────────────────────────────────────────────────────────────────────
    // RENDU
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        UIRenderer.drawBackground(g, width, height);
        UIRenderer.drawPanel(g, panelX, panelY, panelW, panelH);
        renderHeader(g);
        renderSearchBar(g, mouseX, mouseY);
        renderHomeList(g, mouseX, mouseY);
        renderFooter(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphics g) {
        UIRenderer.drawHeader(g, panelX, panelY, panelW, UITheme.HEADER_H);
        UIRenderer.drawTitle(g, font,
                "Home  " + LangManager.getInstance().get("title.homes"),
                panelX + panelW / 2, panelY + 8, UITheme.ACCENT_TITLE);

        String badge = filtered.size() + "/" + allHomes.size();
        UIRenderer.drawBadge(g, font, badge,
                panelX + panelW - UITheme.PAD - font.width(badge) / 2 - 3,
                panelY + 9, UITheme.TEXT_DIM);

        if (HomesManager.getInstance().isWaiting()) {
            long t      = System.currentTimeMillis() / 300 % 4;
            String dots = ".".repeat((int) t + 1);
            g.drawString(font,
                    Component.literal(LangManager.getInstance().get("message.loading") + dots),
                    panelX + UITheme.PAD, panelY + 9,
                    UITheme.TEXT_DIM, false);
        }
    }

    private void renderSearchBar(GuiGraphics g, int mouseX, int mouseY) {
        int sbY = panelY + UITheme.HEADER_H + 4;
        int sbW = panelW - UITheme.PAD * 2 - 24;

        boolean sbHov = mouseX >= listX && mouseX <= listX + sbW
                     && mouseY >= sbY   && mouseY <= sbY + 16;

        g.fill(listX - 2, sbY - 2, listX + sbW + 2, sbY + 18,
                sbHov ? UITheme.BG_HOVER : UITheme.BG_ELEMENT);
        UIRenderer.drawBorder(g, listX - 2, sbY - 2, sbW + 4, 20,
                searchBox != null && searchBox.isFocused()
                        ? UITheme.ACCENT_PRIMARY : UITheme.BORDER_NORMAL);

        int favBtnX = listX + sbW + 4;
        boolean favHov = mouseX >= favBtnX && mouseX <= favBtnX + 20
                      && mouseY >= sbY - 2  && mouseY <= sbY + 18;

        g.fill(favBtnX, sbY - 2, favBtnX + 20, sbY + 18,
                favHov ? UITheme.BG_HOVER : UITheme.BG_ELEMENT);
        UIRenderer.drawBorder(g, favBtnX, sbY - 2, 20, 20,
                showFavOnly ? UITheme.COLOR_GOLD : UITheme.BORDER_NORMAL);
        g.drawString(font,
                Component.literal(showFavOnly ? "*" : "o"),
                favBtnX + 5, sbY + 2,
                showFavOnly ? UITheme.COLOR_GOLD : UITheme.TEXT_DIM, false);
    }

    private void renderHomeList(GuiGraphics g, int mouseX, int mouseY) {
        hoveredIndex = -1;
        ModConfig cfg = ModConfig.getInstance();

        if (filtered.isEmpty()) {
            String msg = allHomes.isEmpty()
                    ? LangManager.getInstance().get("message.no_homes")
                    : LangManager.getInstance().get("message.no_results");
            g.drawString(font, Component.literal(msg),
                    listX + listW / 2 - font.width(msg) / 2,
                    listY + listH / 2 - 4,
                    UITheme.TEXT_DIM, false);
            return;
        }

        int maxScroll = Math.max(0, filtered.size() - maxVisible);
        int endIdx    = Math.min(filtered.size(), scrollOffset + maxVisible);

        for (int i = scrollOffset; i < endIdx; i++) {
            String  home   = filtered.get(i);
            boolean isFav  = cfg.isFavorite(home);
            int     uses   = cfg.getUseCount(home);
            int     rowY   = listY + (i - scrollOffset) * (UITheme.ROW_H + UITheme.ROW_GAP);

            boolean hovered = mouseX >= listX && mouseX <= listX + listW
                           && mouseY >= rowY  && mouseY <= rowY + UITheme.ROW_H;
            if (hovered) hoveredIndex = i;

            UIRenderer.drawRow(g, listX, rowY, listW, UITheme.ROW_H, hovered, isFav);

            // Icône favori
            g.drawString(font,
                    Component.literal(isFav ? "*" : "-"),
                    listX + 5, rowY + 7,
                    isFav ? UITheme.COLOR_GOLD : UITheme.TEXT_DISABLED, false);

            // Nom
            String truncated = truncate(home, listW - 60);
            g.drawString(font, Component.literal(truncated),
                    listX + 16, rowY + 7,
                    hovered ? UITheme.TEXT_PRIMARY : 0xFFCCCCEE, false);

            // Compteur
            if (uses > 0) {
                String useTxt = "x" + uses;
                g.drawString(font, Component.literal(useTxt),
                        listX + listW - font.width(useTxt) - 8,
                        rowY + 7, UITheme.TEXT_DIM, false);
            }
        }

        // Scrollbar
        if (maxScroll > 0) {
            UIRenderer.drawScrollbar(g,
                    listX + listW + 2, listY, listH,
                    scrollOffset, maxScroll);
        }
    }

    private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
        int footerY = panelY + panelH - UITheme.FOOTER_H;
        UIRenderer.drawFooter(g, panelX, footerY, panelW, UITheme.FOOTER_H);

        LangManager lang  = LangManager.getInstance();
        String[] labels   = {
            lang.get("button.refresh"),
            lang.get("button.recent"),
            "Stats",
            lang.get("button.close")
        };
        int btnW   = (panelW - UITheme.PAD * 2 - 3 * 4) / 4;
        int btnY   = footerY + (UITheme.FOOTER_H - 14) / 2;

        for (int i = 0; i < labels.length; i++) {
            int bx  = panelX + UITheme.PAD + i * (btnW + 4);
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

    // ─────────────────────────────────────────────────────────────────────
    // INPUTS — API 1.21.10
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (consumed) return super.mouseClicked(event, true);

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int btn    = event.button();

        // Clic sur un home
        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()) {
            String home = filtered.get(hoveredIndex);
            if (btn == 0) {
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            } else if (btn == 1) {
                ModConfig.getInstance().toggleFavorite(home);
                applyFilter();
                return true;
            }
        }

        // Clic sur le bouton favori (searchbar)
        int sbY     = panelY + UITheme.HEADER_H + 4;
        int sbW     = panelW - UITheme.PAD * 2 - 24;
        int favBtnX = listX + sbW + 4;
        if (btn == 0
         && mouseX >= favBtnX && mouseX <= favBtnX + 20
         && mouseY >= sbY - 2  && mouseY <= sbY + 18) {
            showFavOnly  = !showFavOnly;
            scrollOffset = 0;
            applyFilter();
            return true;
        }

        // Clic sur les boutons du footer
        int footerY = panelY + panelH - UITheme.FOOTER_H;
        int btnW    = (panelW - UITheme.PAD * 2 - 3 * 4) / 4;
        int btnY    = footerY + (UITheme.FOOTER_H - 14) / 2;

        if (btn == 0 && mouseY >= btnY && mouseY <= btnY + 14) {
            for (int i = 0; i < 4; i++) {
                int bx = panelX + UITheme.PAD + i * (btnW + 4);
                if (mouseX >= bx && mouseX <= bx + btnW) {
                    handleFooterClick(i);
                    return true;
                }
            }
        }

        return super.mouseClicked(event, false);
    }

    private void handleFooterClick(int index) {
        if (minecraft == null) return;
        switch (index) {
            case 0 -> refresh();
            case 1 -> minecraft.setScreen(new HistoryScreen(this));
            case 2 -> minecraft.setScreen(new StatsScreen(this));
            case 3 -> minecraft.setScreen(null);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int maxScroll = Math.max(0, filtered.size() - maxVisible);
        scrollOffset  = Math.max(0,
                Math.min(maxScroll, scrollOffset - (int) Math.signum(vScroll)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // ESC
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────

    private String truncate(String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        while (!text.isEmpty() && font.width(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
