package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HistoryScreen extends Screen {

    private static final int COLOR_BG     = 0xEE0A0A1A;
    private static final int COLOR_PANEL  = 0xDD121228;
    private static final int COLOR_ACCENT = 0xFF5B5BFF;
    private static final int COLOR_DIM    = 0xFF8888AA;
    private static final int COLOR_BORDER = 0xFF3A3A7A;

    private final Screen parent;
    private boolean needsRebuild = true;

    public HistoryScreen(Screen parent) {
        super(Component.literal("History"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        needsRebuild = true;
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            rebuildUI();
        }
    }

    private void rebuildUI() {
        clearWidgets();

        int panelX = width / 2 - 130;
        int panelW = 260;
        int pad = 12;
        int listY = 48;

        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        int max = Math.min(history.size(), 12);

        for (int i = 0; i < max; i++) {
            final ModConfig.HistoryEntry entry = history.get(i);
            int btnY = listY + i * 26;
            String label = (i + 1) + ". " + entry.homeName + " §8" + entry.getTimeAgo();

            addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        ModConfig.getInstance().incrementUseCount(entry.homeName);
                        ModConfig.getInstance().addToHistory(entry.homeName);
                        HomesManager.getInstance().teleportToHome(entry.homeName);
                    }
            ).bounds(panelX + pad, btnY, panelW - pad * 2, 22).build());
        }

        int panelH = height - 50;
        int btnY   = 20 + panelH - 22;
        int bW     = 80;
        int clearX = panelX + (panelW / 2) - bW - 4;
        int backX  = panelX + (panelW / 2) + 4;

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.clear")),
                b -> {
                    ModConfig.getInstance().clearHistory();
                    needsRebuild = true;
                }
        ).bounds(clearX, btnY, bW, 16).build());

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.back")),
                b -> { if (minecraft != null) minecraft.setScreen(parent); }
        ).bounds(backX, btnY, bW, 16).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - 130;
        int panelW = 260;
        int panelY = 20;
        int panelH = height - 50;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(ctx, panelX, panelY, panelW, panelH, COLOR_BORDER);

        ctx.drawCenteredString(font,
                Component.literal("⟳ " + LangManager.getInstance().get("title.history")),
                width / 2, panelY + 8, COLOR_ACCENT);

        if (ModConfig.getInstance().getHistory().isEmpty()) {
            ctx.drawCenteredString(font,
                    Component.literal("§7" + LangManager.getInstance().get("message.no_history")),
                    width / 2, panelY + 80, COLOR_DIM);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
