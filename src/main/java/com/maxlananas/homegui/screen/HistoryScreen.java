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
        int panelX = width / 2 - PANEL_W / 2;
        int panelH = height - 50;
        int bottomY = 20 + panelH - 24;

        listAreaTop = 52;
        listAreaBottom = bottomY - 10;
        visibleRows = Math.max(1, (listAreaBottom - listAreaTop) / ROW_STEP);

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

        g.fill(0, 0, width, height, Theme.BG);

        int panelX = width / 2 - PANEL_W / 2;
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

        if (history.isEmpty()) return;

        int maxScroll = Math.max(0, history.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int listX = panelX + PAD;
        int listW = PANEL_W - PAD * 2;

        boolean anyHovered = false;

        int renderStart = Math.max(0, scrollOffset - 1);
        int renderEnd = Math.min(history.size(), scrollOffset + visibleRows + 1);

        for (int i = renderStart; i < renderEnd; i++) {
            ModConfig.HistoryEntry entry = history.get(i);
            int localIdx = i - scrollOffset;
            int rowY = listAreaTop + localIdx * ROW_STEP;

            if (rowY + ROW_H < listAreaTop - ROW_H || rowY > listAreaBottom + ROW_H) continue;

            boolean inArea = rowY >= listAreaTop && rowY + ROW_H <= listAreaBottom;
            boolean hovered = inArea && mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) anyHovered = true;

            int bg = hovered ? Theme.BTN_HOV : Theme.CARD;
            int border = hovered ? Theme.ACCENT : Theme.BORDER;

            g.fill(listX, rowY, listX + listW, rowY + ROW_H, bg);
            Theme.fillBorder(g, listX, rowY, listW, ROW_H, border);

            g.drawString(f, Component.literal("§8" + (i + 1) + "."), listX + 5, rowY + 7, Theme.FAINT);

            String name = Theme.truncate(f, entry.homeName, listW - f.width(entry.getTimeAgo()) - 40);
            g.drawString(f, Component.literal(name), listX + 24, rowY + 7,
                    hovered ? 0xFFFFFFFF : Theme.TEXT);

            g.drawString(f, Component.literal("§8" + entry.getTimeAgo()),
                    listX + listW - f.width(entry.getTimeAgo()) - 6, rowY + 7, Theme.FAINT);
        }

        if (history.size() > visibleRows) {
            int sbX = panelX + PANEL_W - PAD + 4;
            int sbH = listAreaBottom - listAreaTop;
            int thumbH = Math.max(20, sbH * visibleRows / history.size());
            int thumbY = listAreaTop + (sbH - thumbH) * scrollOffset / Math.max(1, history.size() - visibleRows);

            g.fill(sbX, listAreaTop, sbX + 4, listAreaBottom, Theme.CARD);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, Theme.ACCENT_DIM);
            Theme.fillBorder(g, sbX, thumbY, 4, thumbH, Theme.ACCENT);
        }

        if (anyHovered) {
            String tip = L.get("message.click_to_tp");
            int tx = Math.min(mouseX + 14, width - f.width(tip) - 12);
            int ty = Math.max(mouseY - 14, 4);
            g.fill(tx - 3, ty - 3, tx + f.width(tip) + 7, ty + 14, 0xF0100010);
            Theme.fillBorder(g, tx - 3, ty - 3, f.width(tip) + 10, 17, Theme.ACCENT);
            g.drawString(f, Component.literal("§7" + tip), tx, ty, 0xAAAAAA);
        }
    }

    private int hoveredIndex(int mx, int my) {
        int listX = width / 2 - PANEL_W / 2 + PAD;
        int listW = PANEL_W - PAD * 2;
        if (mx < listX || mx > listX + listW) return -1;
        if (my < listAreaTop || my > listAreaBottom) return -1;
        int localIdx = (my - listAreaTop) / ROW_STEP;
        int realIdx = localIdx + scrollOffset;
        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        if (realIdx < 0 || realIdx >= history.size()) return -1;
        int rowY = listAreaTop + localIdx * ROW_STEP;
        if (my < rowY || my > rowY + ROW_H) return -1;
        return realIdx;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        if (history.size() <= visibleRows) return false;
        int maxScroll = history.size() - visibleRows;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int idx = hoveredIndex((int) mouseX, (int) mouseY);
        if (idx >= 0) {
            List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
            if (idx < history.size()) {
                String home = history.get(idx).homeName;
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
