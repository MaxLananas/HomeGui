package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomeGuiClient;
import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.config.SortMode;
import com.maxlananas.homegui.widget.StyledButton;
import com.maxlananas.homegui.widget.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    private static final int PANEL_W  = 320;
    private static final int PAD      = 16;
    private static final int SEARCH_Y  = 60;
    private static final int TOOLBAR_Y = SEARCH_Y + 24;
    private static final int ROW_H    = 22;
    private static final int ROW_GAP  = 3;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int GRID_COLS = 3;
    private static final int GRID_CARD_W = 88;
    private static final int GRID_CARD_H = 48;
    private static final int GRID_GAP = 4;

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
        if (needsRebuild) { needsRebuild = false; rebuildUI(); }
    }

    private void applyFilter() {
        filtered.clear();
        String q = savedSearch.toLowerCase();
        for (String h : allHomes) {
            boolean match = q.isEmpty() || h.toLowerCase().contains(q);
            boolean fav = !showFavOnly || ModConfig.getInstance().isFavorite(h);
            if (match && fav) filtered.add(h);
        }
        SortMode.fromString(ModConfig.getInstance().getSortMode()).apply(filtered);
    }

    private boolean isGrid() {
        return "grid".equals(ModConfig.getInstance().getViewMode());
    }

    private void rebuildUI() {
        clearWidgets();
        ModConfig cfg = ModConfig.getInstance();
        LangManager L = LangManager.getInstance();

        panelX = width / 2 - PANEL_W / 2;
        int searchW = PANEL_W - PAD * 2 - 26;
        int searchY = SEARCH_Y;

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
        int toolbarY = searchY + 24;
        listAreaTop = toolbarY + 22;
        listAreaBottom = bottomY - 10;

        if (isGrid()) {
            int gridRows = Math.max(1, (listAreaBottom - listAreaTop) / (GRID_CARD_H + GRID_GAP));
            visibleRows = gridRows * GRID_COLS;
        } else {
            visibleRows = Math.max(1, (listAreaBottom - listAreaTop) / ROW_STEP);
        }

        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        if (isGrid()) {
            buildGridWidgets();
        } else {
            buildListWidgets();
        }

        int bW = 48, gap = 4;
        int btnCount = 6;
        int total = bW * btnCount + gap * (btnCount - 1);
        int startX = panelX + (PANEL_W - total) / 2;
        SortMode currentSort = SortMode.fromString(cfg.getSortMode());

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
        addRenderableWidget(new StyledButton(startX + (bW + gap) * 3, bottomY, bW, 18, L.get("button.settings"),
                () -> { if (minecraft != null) minecraft.setScreen(new SettingsScreen(this)); }));
        addRenderableWidget(new StyledButton(startX + (bW + gap) * 4, bottomY, bW, 18, L.get("button.export"),
                () -> { cfg.exportData(); }));
        addRenderableWidget(new StyledButton(startX + (bW + gap) * 5, bottomY, bW, 18, L.get("button.close"),
                () -> { if (minecraft != null) minecraft.setScreen(null); }));

        int sortW = PANEL_W - PAD * 2;
        addRenderableWidget(new StyledButton(panelX + PAD, toolbarY, sortW / 2 - 2, 18,
                "⟳ " + L.get(currentSort.langKey),
                () -> {
                    cfg.setSortMode(currentSort.next().name());
                    needsRebuild = true;
                }));
        addRenderableWidget(new StyledButton(panelX + PAD + sortW / 2 + 2, toolbarY, sortW / 2 - 2, 18,
                isGrid() ? "▦ " + L.get("view.grid") : "≡ " + L.get("view.list"),
                () -> {
                    cfg.setViewMode(isGrid() ? "list" : "grid");
                    needsRebuild = true;
                }));
    }

    private void buildListWidgets() {
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
                    () -> HomesManager.getInstance().teleportToHome(home)));

            addRenderableWidget(new StyledButton(listX + mainBtnW + 4, rowY, 22, ROW_H,
                    isFav ? "★" : "☆",
                    () -> { ModConfig.getInstance().toggleFavorite(home); needsRebuild = true; },
                    isFav ? 0xFF3A2A00 : Theme.BTN,
                    isFav ? 0xFF5A4A10 : Theme.BTN_HOV,
                    isFav ? Theme.GOLD : Theme.BORDER,
                    isFav ? Theme.GOLD : Theme.DIM,
                    isFav ? Theme.GOLD : Theme.TEXT));
        }
    }

    private void buildGridWidgets() {
        int totalGridW = GRID_COLS * GRID_CARD_W + (GRID_COLS - 1) * GRID_GAP;
        int gridStartX = panelX + (PANEL_W - totalGridW) / 2;
        int start = scrollOffset;
        int end = Math.min(filtered.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            final String home = filtered.get(i);
            int localIdx = i - scrollOffset;
            int col = localIdx % GRID_COLS;
            int row = localIdx / GRID_COLS;
            int cx = gridStartX + col * (GRID_CARD_W + GRID_GAP);
            int cy = listAreaTop + row * (GRID_CARD_H + GRID_GAP);

            addRenderableWidget(new StyledButton(cx, cy, GRID_CARD_W, GRID_CARD_H, home,
                    () -> HomesManager.getInstance().teleportToHome(home)));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        LangManager L = LangManager.getInstance();
        Font f = font;

        g.fill(0, 0, width, height, Theme.backdrop());
        int panelY = 20;
        int panelH = height - 50;

        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);
        Theme.drawTextCentered(g, f, "✦ " + L.get("title.homes") + " ✦",
                width / 2, panelY + 10, Theme.ACCENT);
        Theme.drawTextCentered(g, f, "§8" + allHomes.size() + " " + L.get("stats.total_homes"),
                width / 2, panelY + 22, Theme.DIM);
        Theme.drawSeparator(g, panelX + PAD, SEARCH_Y - 7, PANEL_W - PAD * 2);

        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(f, Component.literal("§7" + L.get("hint.search")),
                    panelX + PAD + 4, SEARCH_Y + 4, Theme.FAINT);
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

        int toolbarY = TOOLBAR_Y;
        Theme.drawSeparator(g, panelX + PAD, toolbarY + 20, PANEL_W - PAD * 2);
        int bottomY = 20 + panelH - 24;
        Theme.drawSeparator(g, panelX + PAD, bottomY - 10, PANEL_W - PAD * 2);

        if (isGrid()) {
            renderGridOverlay(g, f);
        } else {
            renderListOverlay(g, f);
        }

        if (filtered.size() > visibleRows) {
            int sbX = panelX + PANEL_W - PAD + 4;
            int sbH = listAreaBottom - listAreaTop;
            int thumbH = Math.max(20, sbH * visibleRows / filtered.size());
            int maxScroll = Math.max(1, filtered.size() - visibleRows);
            int thumbY = listAreaTop + (sbH - thumbH) * scrollOffset / maxScroll;
            g.fill(sbX, listAreaTop, sbX + 4, listAreaBottom, Theme.bg(Theme.CARD));
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, Theme.bg(Theme.ACCENT_DIM));
            Theme.fillBorder(g, sbX, thumbY, 4, thumbH, Theme.ACCENT);
        }
    }

    private void renderListOverlay(GuiGraphics g, Font f) {
        int mainBtnW = PANEL_W - PAD * 2 - 28;
        int listX = panelX + PAD;
        int start = scrollOffset;
        int end = Math.min(filtered.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            String home = filtered.get(i);
            int rowY = listAreaTop + (i - scrollOffset) * ROW_STEP;
            boolean isFav = ModConfig.getInstance().isFavorite(home);
            if (isFav) {
                g.fill(listX, rowY, listX + 3, rowY + ROW_H, Theme.bg(Theme.GOLD));
            }
        }
    }

    private void renderGridOverlay(GuiGraphics g, Font f) {
        ModConfig cfg = ModConfig.getInstance();
        int totalGridW = GRID_COLS * GRID_CARD_W + (GRID_COLS - 1) * GRID_GAP;
        int gridStartX = panelX + (PANEL_W - totalGridW) / 2;
        int start = scrollOffset;
        int end = Math.min(filtered.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            String home = filtered.get(i);
            int localIdx = i - scrollOffset;
            int col = localIdx % GRID_COLS;
            int row = localIdx / GRID_COLS;
            int cx = gridStartX + col * (GRID_CARD_W + GRID_GAP);
            int cy = listAreaTop + row * (GRID_CARD_H + GRID_GAP);
            boolean isFav = cfg.isFavorite(home);
            int uses = cfg.getUseCount(home);

            if (isFav) g.fill(cx, cy, cx + GRID_CARD_W, cy + 2, Theme.bg(Theme.GOLD));

            String displayName = Theme.truncate(f, home, GRID_CARD_W - 8);
            g.drawCenteredString(f, Component.literal(displayName),
                    cx + GRID_CARD_W / 2, cy + 10, Theme.TEXT);

            String bottom = (isFav ? "★ " : "") + (uses > 0 ? "×" + uses : "");
            if (!bottom.isEmpty()) {
                g.drawCenteredString(f, Component.literal("§8" + bottom),
                        cx + GRID_CARD_W / 2, cy + 32, Theme.DIM);
            }

            ModConfig.HomeCoords coords = cfg.getHomeCoords(home);
            if (coords != null) {
                String pos = coords.x + " " + coords.y + " " + coords.z;
                g.drawCenteredString(f, Component.literal("§8" + pos),
                        cx + GRID_CARD_W / 2, cy + 42, Theme.FAINT);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Pressing the open-GUI keybind again toggles the menu closed, unless the
        // search box is focused (so the key can still be typed into a home name).
        if ((searchBox == null || !searchBox.isFocused()) && HomeGuiClient.matchesOpenKey(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
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
    public void onClose() { if (minecraft != null) minecraft.setScreen(null); }

    @Override
    public boolean isPauseScreen() { return false; }
}
