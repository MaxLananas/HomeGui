package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.widget.StyledButton;
import com.maxlananas.homegui.widget.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PAD     = 16;

    private EditBox searchBox;
    private String savedSearch = "";

    private final List<String> allHomes  = new ArrayList<>();
    private final List<String> filtered  = new ArrayList<>();
    private boolean showFavOnly = false;
    private boolean needsRebuild = true;

    // Track rendered home buttons for overlays
    private final List<StyledButton> homeButtons = new ArrayList<>();
    private final List<StyledButton> starButtons = new ArrayList<>();
    private final List<String>       displayedHomes = new ArrayList<>();

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
            boolean fav   = !showFavOnly || ModConfig.getInstance().isFavorite(h);
            if (match && fav) filtered.add(h);
        }
    }

    private void rebuildUI() {
        clearWidgets();
        homeButtons.clear();
        starButtons.clear();
        displayedHomes.clear();

        int panelX = width / 2 - PANEL_W / 2;
        int searchW = PANEL_W - PAD * 2 - 26;
        int searchY = 50;

        // ── Search box ────────────────────────────
        searchBox = new EditBox(font, panelX + PAD, searchY, searchW, 16, Component.literal("Search"));
        searchBox.setValue(savedSearch);
        searchBox.setResponder(t -> { savedSearch = t; needsRebuild = true; });
        addRenderableWidget(searchBox);

        // ── Favorite filter toggle ────────────────
        addRenderableWidget(new StyledButton(
                panelX + PAD + searchW + 4, searchY, 22, 16,
                showFavOnly ? "★" : "☆",
                () -> { showFavOnly = !showFavOnly; needsRebuild = true; },
                showFavOnly ? 0xFF3A2A00 : Theme.BTN,
                showFavOnly ? 0xFF5A4A10 : Theme.BTN_HOV,
                showFavOnly ? Theme.GOLD : Theme.BORDER,
                showFavOnly ? Theme.GOLD : Theme.DIM,
                showFavOnly ? Theme.GOLD : Theme.TEXT));

        // ── Home list ─────────────────────────────
        applyFilter();
        int listY = searchY + 24;
        int mainBtnW = PANEL_W - PAD * 2 - 28;
        int maxVisible = Math.min(filtered.size(), 20);

        for (int i = 0; i < maxVisible; i++) {
            final String home = filtered.get(i);
            int btnY = listY + i * 24;

            // Home button
            StyledButton homeBtn = new StyledButton(panelX + PAD, btnY, mainBtnW, 20, home,
                    () -> {
                        ModConfig.getInstance().incrementUseCount(home);
                        ModConfig.getInstance().addToHistory(home);
                        HomesManager.getInstance().teleportToHome(home);
                    });
            addRenderableWidget(homeBtn);
            homeButtons.add(homeBtn);
            displayedHomes.add(home);

            // Star toggle
            boolean isFav = ModConfig.getInstance().isFavorite(home);
            StyledButton starBtn = new StyledButton(
                    panelX + PAD + mainBtnW + 4, btnY, 22, 20,
                    isFav ? "★" : "☆",
                    () -> { ModConfig.getInstance().toggleFavorite(home); needsRebuild = true; },
                    isFav ? 0xFF3A2A00 : Theme.BTN,
                    isFav ? 0xFF5A4A10 : Theme.BTN_HOV,
                    isFav ? Theme.GOLD : Theme.BORDER,
                    isFav ? Theme.GOLD : Theme.DIM,
                    isFav ? Theme.GOLD : Theme.TEXT);
            addRenderableWidget(starBtn);
            starButtons.add(starBtn);
        }

        // ── Bottom nav buttons ────────────────────
        int panelH = height - 50;
        int bottomY = 20 + panelH - 24;
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

        // Full background
        g.fill(0, 0, width, height, Theme.BG);

        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 20;
        int panelH = height - 50;

        // Panel
        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);

        // Title
        Theme.drawTextCentered(g, f, "✦ " + L.get("title.homes") + " ✦",
                width / 2, panelY + 10, Theme.ACCENT);

        // Subtitle: home count
        Theme.drawTextCentered(g, f, "§8" + allHomes.size() + " " + L.get("stats.total_homes"),
                width / 2, panelY + 24, Theme.DIM);

        // Search hint (when empty and unfocused)
        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(f, Component.literal("§7" + L.get("hint.search")),
                    searchBox.getX() + 4, searchBox.getY() + 4, Theme.FAINT);
        }

        // Separator
        Theme.drawSeparator(g, panelX + PAD, 46, PANEL_W - PAD * 2);

        // Empty state
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

        // ── Render all widgets (buttons, search box) ──
        super.render(g, mouseX, mouseY, delta);

        // ── Overlays drawn ON TOP of widgets ──────

        // Gold left accent on favorite home buttons + use count
        for (int i = 0; i < homeButtons.size(); i++) {
            StyledButton btn = homeButtons.get(i);
            String home = displayedHomes.get(i);
            boolean isFav = ModConfig.getInstance().isFavorite(home);
            int uses = ModConfig.getInstance().getUseCount(home);
            int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();

            if (isFav) {
                g.fill(bx, by, bx + 3, by + bh, Theme.GOLD);
            }
            if (uses > 0) {
                String cnt = "×" + uses;
                g.drawString(f, Component.literal("§8" + cnt),
                        bx + bw - f.width(cnt) - 5, by + (bh - 8) / 2, Theme.FAINT);
            }
        }

        // Separator above bottom buttons
        int bottomY = 20 + panelH - 24;
        Theme.drawSeparator(g, panelX + PAD, bottomY - 8, PANEL_W - PAD * 2);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
