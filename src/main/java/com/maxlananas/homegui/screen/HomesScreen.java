package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.widget.StyledButton;
import com.maxlananas.homegui.widget.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    private static final int PANEL_W  = 300;
    private static final int PAD      = 16;
    private static final int ROW_H    = 22;
    private static final int ROW_GAP  = 3;
    private static final int ROW_STEP = ROW_H + ROW_GAP;

    private EditBox searchBox;
    private String savedSearch = "";
    private final List<String> allHomes  = new ArrayList<>();
    private final List<String> filtered  = new ArrayList<>();
    private boolean showFavOnly = false;
    private boolean needsRebuild = true;
    private int scrollOffset = 0;
    private int visibleRows = 0;
    private int listAreaTop = 0;
    private int listAreaBottom = 0;
    private int panelX = 0;

    public HomesScreen() {
        super(Component.literal("HomeGUI"));
    }

    @Override
    protected void init() {
        if (searchBox != null) savedSearch = searchBox.getValue();
        allHomes.clear();
        allHomes.addAll(HomesManager.getInstance().getHomes());
        needsRebuild = true;
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            rebuildUI();
        }
    }

    private void applyFilter() {
        filtered.clear();
        String q = savedSearch.toLowerCase();
        for (String h : allHomes) {
            boolean match = q.isEmpty() || h.toLowerCase().contains(q);
            boolean fav = !showFavOnly || ModConfig.getInstance().isFavorite(h);
            if (match && fav) filtered.add(h);
        }
    }

    private void rebuildUI() {
        clearWidgets();

        panelX = width / 2 - PANEL_W / 2;
        int searchW = PANEL_W - PAD * 2 - 26;
        int searchY = 50;

        searchBox = new EditBox(font, panelX + PAD, searchY, searchW, 16, Component.literal("Search"));
        searchBox.setValue(savedSearch);
        searchBox.setResponder(t -> { savedSearch = t; needsRebuild = true; });
        addRenderableWidget(searchBox);

        addRenderableWidget(new StyledButton(
                panelX + PAD + searchW + 4, searchY, 22, 16,
                showFavOnly ? "★" : "☆",
                () -> { showFavOnly = !showFavOnly; needsRebuild = true; },
                showFavOnly ? 0xFF3A2A00 : Theme.BTN,
                showFavOnly ? 0xFF5A4A10 : Theme.BTN_HOV,
                showFavOnly ? Theme.GOLD : Theme.BORDER,
                showFavOnly ? Theme.GOLD : Theme.DIM,
                showFavOnly ? Theme.GOLD : Theme.TEXT));

        applyFilter();

        int panelH = height - 50;
        int bottomY = 20 + panelH - 24;
        listAreaTop = searchY + 24;
        listAreaBottom = bottomY - 10;
        visibleRows = Math.max(1, (listAreaBottom - listAreaTop) / ROW_STEP);

        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int mainBtnW = PANEL_W - PAD * 2 - 28;
        int listX = panelX + PAD;

        int start = scrollOffset;
        int end = Math.min(filtered.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            final String home = filtered.get(i);
            int rowY = listAreaTop + (i - scrollOffset) * ROW_STEP;
            boolean isFav = ModConfig.getInstance().isFavorite(home);
            int uses = ModConfig.getInstance().getUseCount(home);

            String label = (isFav ? "★ " : "") + home + (uses > 0 ? "  ×" + uses : "");

            addRenderableWidget(new StyledButton(listX, rowY, mainBtnW, ROW_H, label,
                    () -> {
                        ModConfig.getInstance().incrementUseCount(home);
                        ModConfig.getInstance().addToHistory(home);
                        HomesManager.getInstance().teleportToHome(home);
                    }));

            addRenderableWidget(new StyledButton(listX + mainBtnW + 4, rowY, 22, ROW_H,
                    isFav ? "★" : "☆",
                    () -> { ModConfig.getInstance().toggleFavorite(home); needsRebuild = true; },
                    isFav ? 0xFF3A2A00 : Theme.BTN,
                    isFav ? 0xFF5A4A10 : Theme.BTN_HOV,
                    isFav ? Theme.GOLD : Theme.BORDER,
                    isFav ? Theme.GOLD : Theme.DIM,
                    isFav ? Theme.GOLD : Theme.TEXT));
        }

        int bW = 60, gap = 6;
        int total = bW * 4 + gap * 3;
        int startX = panelX + (PANEL_W - total) / 2;
        LangManager L = LangManager.getInstance();

        addRenderableWidget(new StyledButton(startX, bottomY, bW, 18, L.get("button.refresh"),
                () -> {
                    HomesManager.getInstance().requestHomes();
                    allHomes.clear();
                    allHomes.addAll(HomesManager.getInstance().getHomes());
                    needsRebuild = true;
                }));
        addRenderableWidget(new StyledButton(startX + bW + gap, bottomY, bW, 18, L.get("button.recent"),
                () -> { if (minecraft != null) minecraft.setScreen(new HistoryScreen(this)); }));
        addRenderableWidget(new StyledButton(startX + (bW + gap) * 2, bottomY, bW, 18, "Stats",
                () -> { if (minecraft != null) minecraft.setScreen(new StatsScreen(this)); }));
        addRenderableWidget(new StyledButton(startX + (bW + gap) * 3, bottomY, bW, 18, L.get("button.close"),
                () -> { if (minecraft != null) minecraft.setScreen(null); }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        LangManager L = LangManager.getInstance();
        Font f = font;

        g.fill(0, 0, width, height, Theme.BG);

        int panelY = 20;
        int panelH = height - 50;

        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);
        Theme.drawTextCentered(g, f, "✦ " + L.get("title.homes") + " ✦",
                width / 2, panelY + 10, Theme.ACCENT);
        Theme.drawTextCentered(g, f, "§8" + allHomes.size() + " " + L.get("stats.total_homes"),
                width / 2, panelY + 24, Theme.DIM);
        Theme.drawSeparator(g, panelX + PAD, 46, PANEL_W - PAD * 2);

        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(f, Component.literal("§7" + L.get("hint.search")),
                    panelX + PAD + 4, 54, Theme.FAINT);
        }

        if (filtered.isEmpty()) {
            int cy = 120;
            if (allHomes.isEmpty()) {
                Theme.drawTextCentered(g, f, "§7☁  " + L.get("message.no_homes"), width / 2, cy, Theme.DIM);
                Theme.drawTextCentered(g, f, "§8" + L.get("hint.create_home"), width / 2, cy + 16, Theme.FAINT);
            } else {
                Theme.drawTextCentered(g, f, "§7🔍  " + L.get("message.no_results") + " \"" + savedSearch + "\"",
                        width / 2, cy, Theme.DIM);
            }
        }

        super.render(g, mouseX, mouseY, delta);

        int bottomY = 20 + panelH - 24;
        Theme.drawSeparator(g, panelX + PAD, bottomY - 10, PANEL_W - PAD * 2);

        if (filtered.size() > visibleRows) {
            int sbX = panelX + PANEL_W - PAD + 4;
            int sbH = listAreaBottom - listAreaTop;
            int thumbH = Math.max(20, sbH * visibleRows / filtered.size());
            int maxScroll = Math.max(1, filtered.size() - visibleRows);
            int thumbY = listAreaTop + (sbH - thumbH) * scrollOffset / maxScroll;
            g.fill(sbX, listAreaTop, sbX + 4, listAreaBottom, Theme.CARD);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, Theme.ACCENT_DIM);
            Theme.fillBorder(g, sbX, thumbY, 4, thumbH, Theme.ACCENT);
        }

        if (filtered.size() > visibleRows) {
            String hint = "§8↑↓";
            g.drawString(f, Component.literal(hint),
                    panelX + PANEL_W - PAD - 10, listAreaBottom + 4, Theme.FAINT);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (filtered.size() <= visibleRows) return false;
        int maxScroll = filtered.size() - visibleRows;
        int newOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            needsRebuild = true;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
