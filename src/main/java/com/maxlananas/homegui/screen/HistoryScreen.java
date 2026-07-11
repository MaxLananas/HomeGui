package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.widget.StyledButton;
import com.maxlananas.homegui.widget.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HistoryScreen extends Screen {

    private static final int PANEL_W  = 280;
    private static final int PAD      = 12;
    private static final int ROW_H    = 22;
    private static final int ROW_GAP  = 3;
    private static final int ROW_STEP = ROW_H + ROW_GAP;

    private final Screen parent;
    private boolean needsRebuild = true;
    private int scrollOffset = 0;
    private int visibleRows = 0;
    private int listAreaTop = 0;
    private int listAreaBottom = 0;
    private int panelX = 0;

    public HistoryScreen(Screen parent) {
        super(Component.literal("History"));
        this.parent = parent;
    }

    @Override
    protected void init() { needsRebuild = true; }

    @Override
    public void tick() {
        if (needsRebuild) { needsRebuild = false; rebuildUI(); }
    }

    private void rebuildUI() {
        clearWidgets();
        LangManager L = LangManager.getInstance();

        panelX = width / 2 - PANEL_W / 2;
        int panelH = height - 50;
        int bottomY = 20 + panelH - 24;

        listAreaTop = 52;
        listAreaBottom = bottomY - 10;
        visibleRows = Math.max(1, (listAreaBottom - listAreaTop) / ROW_STEP);

        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();

        int maxScroll = Math.max(0, history.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int listX = panelX + PAD;
        int listW = PANEL_W - PAD * 2 - 44;

        int start = scrollOffset;
        int end = Math.min(history.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            final ModConfig.HistoryEntry entry = history.get(i);
            int rowY = listAreaTop + (i - scrollOffset) * ROW_STEP;

            String label = (i + 1) + ". " + entry.homeName;

            addRenderableWidget(new StyledButton(listX, rowY, listW, ROW_H, label,
                    () -> {
                        ModConfig.getInstance().incrementUseCount(entry.homeName);
                        ModConfig.getInstance().addToHistory(entry.homeName);
                        HomesManager.getInstance().teleportToHome(entry.homeName);
                    }));
        }

        int bW = 90;
        int clearX = panelX + (PANEL_W / 2) - bW - 6;
        int backX  = panelX + (PANEL_W / 2) + 6;

        addRenderableWidget(new StyledButton(clearX, bottomY, bW, 18, L.get("button.clear"),
                () -> { ModConfig.getInstance().clearHistory(); needsRebuild = true; },
                Theme.BTN, 0xFF3A1A1A, Theme.DANGER, Theme.DIM, Theme.DANGER));

        addRenderableWidget(new StyledButton(backX, bottomY, bW, 18, L.get("button.back"),
                () -> { if (minecraft != null) minecraft.setScreen(parent); }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        LangManager L = LangManager.getInstance();
        Font f = font;

        g.fill(0, 0, width, height, Theme.backdrop());

        int panelY = 20;
        int panelH = height - 50;

        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);
        Theme.drawTextCentered(g, f, "⟳ " + L.get("title.history"),
                width / 2, panelY + 10, Theme.ACCENT);

        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();

        if (history.isEmpty()) {
            Theme.drawTextCentered(g, f, "§7☁  " + L.get("message.no_history"),
                    width / 2, panelY + 80, Theme.DIM);
        }

        super.render(g, mouseX, mouseY, delta);

        int listX = panelX + PAD;
        int listW = PANEL_W - PAD * 2 - 44;
        int timeX = listX + listW + 4;

        int start = scrollOffset;
        int end = Math.min(history.size(), scrollOffset + visibleRows);

        for (int i = start; i < end; i++) {
            ModConfig.HistoryEntry entry = history.get(i);
            int rowY = listAreaTop + (i - scrollOffset) * ROW_STEP;
            g.drawString(f, Component.literal("§8" + entry.getTimeAgo()),
                    timeX, rowY + 7, Theme.FAINT);
        }

        if (history.size() > visibleRows) {
            int sbX = panelX + PANEL_W - PAD + 4;
            int sbH = listAreaBottom - listAreaTop;
            int thumbH = Math.max(20, sbH * visibleRows / history.size());
            int maxScroll = Math.max(1, history.size() - visibleRows);
            int thumbY = listAreaTop + (sbH - thumbH) * scrollOffset / maxScroll;
            g.fill(sbX, listAreaTop, sbX + 4, listAreaBottom, Theme.CARD);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, Theme.ACCENT_DIM);
            Theme.fillBorder(g, sbX, thumbY, 4, thumbH, Theme.ACCENT);
        }

        int bottomY = 20 + panelH - 24;
        Theme.drawSeparator(g, panelX + PAD, bottomY - 10, PANEL_W - PAD * 2);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        if (history.size() <= visibleRows) return false;
        int maxScroll = history.size() - visibleRows;
        int newOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            needsRebuild = true;
        }
        return true;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
