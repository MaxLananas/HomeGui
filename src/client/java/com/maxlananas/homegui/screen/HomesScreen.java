package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {
    private static final int COLOR_BG           = 0xEE0A0A1A;
    private static final int COLOR_PANEL        = 0xDD121228;
    private static final int COLOR_ACCENT       = 0xFF5B5BFF;
    private static final int COLOR_ACCENT_HOVER = 0xFF8888FF;
    private static final int COLOR_FAV          = 0xFFFFD700;
    private static final int COLOR_TEXT         = 0xFFE0E0FF;
    private static final int COLOR_TEXT_DIM     = 0xFF8888AA;
    private static final int COLOR_BTN          = 0xFF1E1E3F;
    private static final int COLOR_BTN_HOVER    = 0xFF2E2E5F;
    private static final int COLOR_BORDER       = 0xFF3A3A7A;

    private static final int BUTTON_H  = 22;
    private static final int BUTTON_W  = 220;
    private static final int SPACING   = 4;
    private static final int PANEL_PAD = 16;

    private final List<String> homes        = new ArrayList<>();
    private final List<String> filtered     = new ArrayList<>();
    private int    hoveredIndex             = -1;
    private String searchQuery              = "";
    private boolean searchFocused           = false;
    private int    scrollOffset             = 0;
    private boolean showFavoritesOnly       = false;

    private int btnHistoryX, btnStatsX, btnRefreshX, btnFavX, btnCloseX;
    private int bottomBtnY;

    public HomesScreen() {
        super(Component.literal("HomeGUI"));
    }

    @Override
    protected void init() {
        homes.clear();
        homes.addAll(HomesManager.getInstance().getHomes());
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        for (String home : homes) {
            boolean matchSearch = searchQuery.isEmpty() ||
                    home.toLowerCase().contains(searchQuery.toLowerCase());
            boolean matchFav = !showFavoritesOnly ||
                    ModConfig.getInstance().isFavorite(home);
            if (matchSearch && matchFav) filtered.add(home);
        }
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - 140;
        int panelW = 280;
        int panelY = 20;
        int panelH = height - 50;
        drawPanel(ctx, panelX, panelY, panelX + panelW, panelY + panelH);

        String title = LangManager.getInstance().get("title.homes");
        ctx.drawCenteredString(font, Component.literal("✦ " + title + " ✦"),
                width / 2, panelY + 8, COLOR_ACCENT);

        int searchY = panelY + 26;
        int searchX = panelX + PANEL_PAD;
        int searchW = panelW - PANEL_PAD * 2 - 28;

        ctx.fill(searchX, searchY, searchX + searchW, searchY + 16,
                searchFocused ? 0xFF1A1A4A : 0xFF111130);
        ctx.renderOutline(searchX, searchY, searchW, 16, searchFocused ? COLOR_ACCENT : COLOR_BORDER);
        String searchDisplay = searchQuery.isEmpty() && !searchFocused
                ? "§7🔍 Search..."
                : "§f" + searchQuery + (searchFocused ? "§7|" : "");
        ctx.drawString(font, Component.literal(searchDisplay),
                searchX + 4, searchY + 4, COLOR_TEXT);

        int favBtnX = searchX + searchW + 4;
        boolean favBtnHover = mouseX >= favBtnX && mouseX <= favBtnX + 22
                && mouseY >= searchY && mouseY <= searchY + 16;
        ctx.fill(favBtnX, searchY, favBtnX + 22, searchY + 16,
                showFavoritesOnly ? 0xFF3A2A00 : (favBtnHover ? COLOR_BTN_HOVER : COLOR_BTN));
        ctx.renderOutline(favBtnX, searchY, 22, 16,
                showFavoritesOnly ? COLOR_FAV : COLOR_BORDER);
        ctx.drawCenteredString(font, Component.literal("★"),
                favBtnX + 11, searchY + 4,
                showFavoritesOnly ? COLOR_FAV : COLOR_TEXT_DIM);

        int listStartY = searchY + 22;
        int listEndY   = panelY + panelH - 30;
        int visibleRows = (listEndY - listStartY) / (BUTTON_H + SPACING);
        hoveredIndex = -1;

        if (filtered.isEmpty()) {
            String msg = homes.isEmpty()
                    ? LangManager.getInstance().get("message.no_homes")
                    : LangManager.getInstance().get("message.no_results") + " \"" + searchQuery + "\"";
            ctx.drawCenteredString(font, Component.literal("§7" + msg),
                    width / 2, listStartY + 30, COLOR_TEXT_DIM);
        } else {
            int maxScroll = Math.max(0, filtered.size() - visibleRows);
            scrollOffset = Math.min(scrollOffset, maxScroll);

            for (int i = 0; i < visibleRows && (i + scrollOffset) < filtered.size(); i++) {
                int idx  = i + scrollOffset;
                String home = filtered.get(idx);
                int btnY = listStartY + i * (BUTTON_H + SPACING);
                int btnX = panelX + PANEL_PAD;
                int btnW = panelW - PANEL_PAD * 2;

                boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW
                        && mouseY >= btnY && mouseY <= btnY + BUTTON_H;
                boolean isFav   = ModConfig.getInstance().isFavorite(home);
                int     uses    = ModConfig.getInstance().getUseCount(home);

                if (hovered) hoveredIndex = idx;
                int bgColor = hovered ? COLOR_ACCENT_HOVER : COLOR_BTN;
                ctx.fill(btnX, btnY, btnX + btnW, btnY + BUTTON_H, bgColor);
                ctx.renderOutline(btnX, btnY, btnW, BUTTON_H,
                        isFav ? COLOR_FAV : (hovered ? COLOR_ACCENT : COLOR_BORDER));
                if (isFav) {
                    ctx.drawString(font, Component.literal("★"),
                            btnX + 5, btnY + 7, COLOR_FAV);
                }
                ctx.drawCenteredString(font, Component.literal(home),
                        btnX + btnW / 2, btnY + 7, hovered ? 0xFFFFFFFF : COLOR_TEXT);
                if (uses > 0) {
                    ctx.drawString(font, Component.literal("§8×" + uses),
                            btnX + btnW - 22, btnY + 7, COLOR_TEXT_DIM);
                }
            }

            if (filtered.size() > visibleRows) {
                int sbX    = panelX + panelW - 6;
                int sbH    = listEndY - listStartY;
                int thumbH = Math.max(16, sbH * visibleRows / filtered.size());
                int thumbY = listStartY + (sbH - thumbH) * scrollOffset
                        / Math.max(1, filtered.size() - visibleRows);
                ctx.fill(sbX, listStartY, sbX + 4, listEndY, 0xFF1A1A3A);
                ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, COLOR_ACCENT);
            }
        }

        bottomBtnY = panelY + panelH - 22;
        renderBottomButtons(ctx, mouseX, mouseY);

        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()) {
            String h = filtered.get(hoveredIndex);
            String tip = LangManager.getInstance().get("message.click_to_tp") + " | "
                    + LangManager.getInstance().get("favorite.right_click");
            ctx.renderTooltip(font, List.of(
                    Component.literal("§b" + h),
                    Component.literal("§7" + tip)
            ), mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPanel(GuiGraphics ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1, y1, x2, y2, COLOR_PANEL);
        ctx.fill(x1, y1, x2, y1 + 1, COLOR_BORDER);
        ctx.fill(x1, y2 - 1, x2, y2, COLOR_BORDER);
        ctx.fill(x1, y1, x1 + 1, y2, COLOR_BORDER);
        ctx.fill(x2 - 1, y1, x2, y2, COLOR_BORDER);
    }

    private void renderBottomButtons(GuiGraphics ctx, int mouseX, int mouseY) {
        int panelX = width / 2 - 140;
        int panelW = 280;
        int bW = 52;
        int bH = 16;
        int gap = 4;
        int totalW = bW * 4 + gap * 3;
        int startX = panelX + (panelW - totalW) / 2;

        String[] labels = {
            LangManager.getInstance().get("button.refresh"),
            LangManager.getInstance().get("button.recent"),
            "Stats",
            LangManager.getInstance().get("button.close")
        };

        btnRefreshX = startX;
        btnHistoryX = startX + bW + gap;
        btnStatsX   = startX + (bW + gap) * 2;
        btnCloseX   = startX + (bW + gap) * 3;

        int[] xs = { btnRefreshX, btnHistoryX, btnStatsX, btnCloseX };

        for (int i = 0; i < 4; i++) {
            boolean hov = mouseX >= xs[i] && mouseX <= xs[i] + bW
                    && mouseY >= bottomBtnY && mouseY <= bottomBtnY + bH;
            ctx.fill(xs[i], bottomBtnY, xs[i] + bW, bottomBtnY + bH,
                    hov ? COLOR_BTN_HOVER : COLOR_BTN);
            ctx.renderOutline(xs[i], bottomBtnY, bW, bH,
                    hov ? COLOR_ACCENT : COLOR_BORDER);
            ctx.drawCenteredString(font, Component.literal(labels[i]),
                    xs[i] + bW / 2, bottomBtnY + 4,
                    hov ? 0xFFFFFFFF : COLOR_TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        int panelX  = width / 2 - 140;
        int panelW  = 280;
        int panelY  = 20;
        int panelH  = height - 50;
        int searchY = panelY + 26;
        int searchX = panelX + PANEL_PAD;
        int searchW = panelW - PANEL_PAD * 2 - 28;
        int favBtnX = searchX + searchW + 4;

        if (mx >= searchX && mx <= searchX + searchW
                && my >= searchY && my <= searchY + 16) {
            searchFocused = !searchFocused;
            return true;
        }

        if (mx >= favBtnX && mx <= favBtnX + 22
                && my >= searchY && my <= searchY + 16) {
            showFavoritesOnly = !showFavoritesOnly;
            applyFilter();
            return true;
        }

        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()) {
            String home = filtered.get(hoveredIndex);
            if (button == 1) {
                ModConfig.getInstance().toggleFavorite(home);
            } else {
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
            }
            return true;
        }

        // Bottom buttons
        if (my >= bottomBtnY && my <= bottomBtnY + 16) {
            if (mx >= btnRefreshX && mx <= btnRefreshX + 52) {
                HomesManager.getInstance().requestHomes();
                homes.clear();
                homes.addAll(HomesManager.getInstance().getHomes());
                applyFilter();
                return true;
            }
            if (mx >= btnHistoryX && mx <= btnHistoryX + 52) {
                assert minecraft != null;
                minecraft.setScreen(new HistoryScreen(this));
                return true;
            }
            if (mx >= btnStatsX && mx <= btnStatsX + 52) {
                assert minecraft != null;
                minecraft.setScreen(new StatsScreen(this));
                return true;
            }
            if (mx >= btnCloseX && mx <= btnCloseX + 52) {
                assert minecraft != null;
                minecraft.setScreen(null);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            assert minecraft != null;
            minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
