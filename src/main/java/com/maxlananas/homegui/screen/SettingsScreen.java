package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.config.SortMode;
import com.maxlananas.homegui.widget.StyledButton;
import com.maxlananas.homegui.widget.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {

    private static final int PANEL_W = 280;
    private final Screen parent;
    private boolean needsRebuild = true;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
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
        ModConfig cfg = ModConfig.getInstance();
        LangManager L = LangManager.getInstance();

        int panelX = width / 2 - PANEL_W / 2;
        int pad = 16;
        int btnW = PANEL_W - pad * 2;
        int y = 60;
        int rowH = 22;
        int gap = 6;

        SortMode currentSort = SortMode.fromString(cfg.getSortMode());

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("settings.language") + ": " + LangManager.getLanguageName(L.getCurrentCode()),
                () -> { L.cycleLanguage(); needsRebuild = true; }));
        y += rowH + gap;

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("settings.sort") + ": " + L.get(currentSort.langKey),
                () -> { cfg.setSortMode(currentSort.next().name()); needsRebuild = true; }));
        y += rowH + gap;

        String viewLabel = "grid".equals(cfg.getViewMode()) ? L.get("view.grid") : L.get("view.list");
        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("settings.view") + ": " + viewLabel,
                () -> { cfg.setViewMode("grid".equals(cfg.getViewMode()) ? "list" : "grid"); needsRebuild = true; }));
        y += rowH + gap;

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("settings.compact") + ": " + (cfg.isCompactMode() ? "ON" : "OFF"),
                () -> { cfg.setCompactMode(!cfg.isCompactMode()); needsRebuild = true; }));
        y += rowH + gap;

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("settings.transparent") + ": " + (cfg.isTransparentMenu() ? "ON" : "OFF"),
                () -> { cfg.setTransparentMenu(!cfg.isTransparentMenu()); needsRebuild = true; }));
        y += rowH + gap + 8;

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW / 2 - 4, rowH,
                L.get("button.export"),
                () -> { cfg.exportData(); }));
        addRenderableWidget(new StyledButton(panelX + pad + btnW / 2 + 4, y, btnW / 2 - 4, rowH,
                L.get("button.import"),
                () -> { cfg.importData(); }));
        y += rowH + gap + 12;

        addRenderableWidget(new StyledButton(panelX + pad, y, btnW, rowH,
                L.get("button.back"),
                () -> { if (minecraft != null) minecraft.setScreen(parent); }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        LangManager L = LangManager.getInstance();
        Font f = font;

        g.fill(0, 0, width, height, Theme.backdrop());

        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 20;
        int panelH = height - 50;

        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);
        Theme.drawTextCentered(g, f, "⚙ " + L.get("button.settings"),
                width / 2, panelY + 10, Theme.ACCENT);

        int pad = 16;
        Theme.drawSeparator(g, panelX + pad, 38, PANEL_W - pad * 2);

        Theme.drawTextCentered(g, f, "§8" + LangManager.getLanguageName(L.getCurrentCode()),
                width / 2, panelY + panelH - 40, Theme.DIM);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
