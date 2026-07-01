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
        scrollOffset = 0;
    }

    private void rebuildUI() {
        clearWidgets();

        int panelX = width / 2 - PANEL_W / 2;
        int searchW = PANEL_W - PAD * 2 - 26;
        int searchY = 50;

        searchBox = new EditBox(font, panelX + PAD, searchY, searchW, 16, Component.literal("Search"));
        searchBox.setValue(savedSearch);
        searchBox.setResponder(t -> {
            savedSearch = t;
            needsRebuild = true;
        });
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

        int panelX = width / 2 - PANEL_W / 2;
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
                    searchX(), 54, Theme.FAINT);
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

        Theme.drawSeparator(g, panelX + PAD, listAreaBottom + 2, PANEL_W - PAD * 2);

        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int mainBtnW = PANEL_W - PAD * 2 - 28;
        int listX = panelX + PAD;
        int areaW = PANEL_W - PAD * 2;

        boolean anyHovered = false;

        int renderStart = Math.max(0, scrollOffset - 1);
        int renderEnd = Math.min(filtered.size(), scrollOffset + visibleRows + 1);

        for (int i = renderStart; i < renderEnd; i++) {
            String home = filtered.get(i);
            int localIdx = i - scrollOffset;
            int rowY = listAreaTop + localIdx * ROW_STEP;

            if (rowY + ROW_H < listAreaTop - ROW_H || rowY > listAreaBottom + ROW_H) continue;

            boolean inArea = rowY >= listAreaTop && rowY + ROW_H <= listAreaBottom;
            boolean hovered = inArea && mouseX >= listX && mouseX <= listX + areaW
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) anyHovered = true;

            boolean isFav = ModConfig.getInstance().isFavorite(home);
            int uses = ModConfig.getInstance().getUseCount(home);

            int bg = hovered ? Theme.BTN_HOV : Theme.BTN;
            int borderColor = isFav ? Theme.GOLD : (hovered ? Theme.ACCENT : Theme.BORDER);

            g.fill(listX, rowY, listX + areaW, rowY + ROW_H, bg);
            Theme.fillBorder(g, listX, rowY, areaW, ROW_H, borderColor);

            if (isFav) {
                g.fill(listX, rowY, listX + 3, rowY + ROW_H, Theme.GOLD);
                g.drawString(f, Component.literal("★"), listX + 6, rowY + 7, Theme.GOLD);
            }

            String displayName = Theme.truncate(f, home, mainBtnW - (isFav ? 24 : 8) - (uses > 0 ? f.width("×" + uses) + 12 : 0));
            int textX = isFav ? listX + 18 : listX + 6;
            g.drawString(f, Component.literal(displayName), textX, rowY + 7,
                    hovered ? 0xFFFFFFFF : Theme.TEXT);

            if (uses > 0) {
                String cnt = "§8×" + uses;
                g.drawString(f, Component.literal(cnt),
                        listX + areaW - f.width(cnt) - 6, rowY + 7, Theme.FAINT);
            }

            if (isFav) {
                String starHov = (mouseX >= listX + areaW - 26 && mouseX <= listX + areaW - 4
                        && mouseY >= rowY + 2 && mouseY <= rowY + ROW_H - 2) ? "★" : "☆";
                if (!starHov.equals("★")) starHov = "☆";
                g.drawString(f, Component.literal("§6" + starHov),
                        listX + areaW - 18, rowY + 7, Theme.GOLD);
            }
        }

        if (filtered.size() > visibleRows) {
            int sbX = panelX + PANEL_W - PAD + 4;
            int sbH = listAreaBottom - listAreaTop;
            int thumbH = Math.max(20, sbH * visibleRows / filtered.size());
            int thumbY = listAreaTop + (sbH - thumbH) * scrollOffset / Math.max(1, filtered.size() - visibleRows);

            g.fill(sbX, listAreaTop, sbX + 4, listAreaBottom, Theme.CARD);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, Theme.ACCENT_DIM);
            Theme.fillBorder(g, sbX, thumbY, 4, thumbH, Theme.ACCENT);
        }

        if (anyHovered && hoveredHome(mouseX, mouseY) != null) {
            String h = hoveredHome(mouseX, mouseY);
            boolean isFav = ModConfig.getInstance().isFavorite(h);
            String tip = L.get("message.click_to_tp") + "  |  " + L.get("message.fav_tip");

            int tw = Math.max(f.width(h), f.width(tip)) + 16;
            int th = 26;
            int tx = Math.min(mouseX + 14, width - tw - 4);
            int ty = Math.max(mouseY - 16, 4);

            g.fill(tx - 4, ty - 4, tx + tw + 4, ty + th + 4, 0xF0100010);
            Theme.fillBorder(g, tx - 4, ty - 4, tw + 8, th + 8, Theme.ACCENT);
            g.fill(tx - 4, ty - 4, tx + tw + 8, ty - 3, Theme.ACCENT);
            g.drawString(f, Component.literal("§b" + h), tx, ty + 2, 0xFFFFFF);
            g.drawString(f, Component.literal("§7" + tip), tx, ty + 14, 0xAAAAAA);
        }
    }

    private int searchX() {
        return width / 2 - PANEL_W / 2 + PAD;
    }

    private int homeListX() {
        return width / 2 - PANEL_W / 2 + PAD;
    }

    private int homeListW() {
        return PANEL_W - PAD * 2;
    }

    private String hoveredHome(int mx, int my) {
        if (mx < homeListX() || mx > homeListX() + homeListW()) return null;
        if (my < listAreaTop || my > listAreaBottom) return null;
        int localIdx = (my - listAreaTop) / ROW_STEP;
        int realIdx = localIdx + scrollOffset;
        if (realIdx < 0 || realIdx >= filtered.size()) return null;
        int rowY = listAreaTop + localIdx * ROW_STEP;
        if (my < rowY || my > rowY + ROW_H) return null;
        return filtered.get(realIdx);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (filtered.size() <= visibleRows) return false;
        int maxScroll = filtered.size() - visibleRows;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String home = hoveredHome((int) mouseX, (int) mouseY);
        if (home != null) {
            int listAreaRight = homeListX() + homeListW();
            boolean isFav = ModConfig.getInstance().isFavorite(home);

            if (button == 1) {
                ModConfig.getInstance().toggleFavorite(home);
                needsRebuild = true;
                return true;
            }

            if (isFav && mouseX >= listAreaRight - 26 && mouseX <= listAreaRight - 4) {
                ModConfig.getInstance().toggleFavorite(home);
                needsRebuild = true;
                return true;
            }

            ModConfig.getInstance().incrementUseCount(home);
            ModConfig.getInstance().addToHistory(home);
            HomesManager.getInstance().teleportToHome(home);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
