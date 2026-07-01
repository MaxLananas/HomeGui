package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    private static final int COLOR_BG     = 0xEE0A0A1A;
    private static final int COLOR_PANEL  = 0xDD121228;
    private static final int COLOR_ACCENT = 0xFF5B5BFF;
    private static final int COLOR_DIM    = 0xFF8888AA;
    private static final int COLOR_BORDER = 0xFF3A3A7A;
    private static final int COLOR_FAV    = 0xFFFFD700;

    private static final int PANEL_W = 280;
    private static final int PAD     = 16;

    private EditBox searchBox;
    private final List<String> allHomes  = new ArrayList<>();
    private final List<String> filtered  = new ArrayList<>();
    private boolean showFavOnly = false;
    private boolean needsRebuild = true;

    public HomesScreen() {
        super(Component.literal("HomeGUI"));
    }

    @Override
    protected void init() {
        allHomes.clear();
        allHomes.addAll(HomesManager.getInstance().getHomes());
        needsRebuild = true;
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            rebuildWidgets();
        }
    }

    private void applyFilter() {
        filtered.clear();
        String q = (searchBox != null) ? searchBox.getValue().toLowerCase() : "";
        for (String h : allHomes) {
            boolean match = q.isEmpty() || h.toLowerCase().contains(q);
            boolean fav   = !showFavOnly || ModConfig.getInstance().isFavorite(h);
            if (match && fav) filtered.add(h);
        }
    }

    private void rebuildWidgets() {
        String prevQuery = (searchBox != null) ? searchBox.getValue() : "";
        clearWidgets();

        int panelX = width / 2 - PANEL_W / 2;
        int searchW = PANEL_W - PAD * 2 - 26;
        int searchY = 48;

        // Search box
        searchBox = new EditBox(font, panelX + PAD, searchY, searchW, 16, Component.literal("Search"));
        searchBox.setValue(prevQuery);
        searchBox.setResponder(t -> needsRebuild = true);
        addRenderableWidget(searchBox);

        // Favorite filter toggle
        addRenderableWidget(Button.builder(
                Component.literal(showFavOnly ? "★" : "☆"),
                b -> { showFavOnly = !showFavOnly; needsRebuild = true; }
        ).bounds(panelX + PAD + searchW + 4, searchY, 22, 16).build());

        // Home list
        applyFilter();
        int listY = searchY + 22;
        int maxVisible = Math.min(filtered.size(), 20);
        int mainBtnW = PANEL_W - PAD * 2 - 26;

        for (int i = 0; i < maxVisible; i++) {
            final String home = filtered.get(i);
            boolean isFav = ModConfig.getInstance().isFavorite(home);
            int uses = ModConfig.getInstance().getUseCount(home);

            String label = (isFav ? "★ " : "") + home + (uses > 0 ? " §8×" + uses : "");
            int btnY = listY + i * 24;

            // TP button
            addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        ModConfig.getInstance().incrementUseCount(home);
                        ModConfig.getInstance().addToHistory(home);
                        HomesManager.getInstance().teleportToHome(home);
                    }
            ).bounds(panelX + PAD, btnY, mainBtnW, 20).build());

            // Star button
            addRenderableWidget(Button.builder(
                    Component.literal(isFav ? "★" : "☆"),
                    b -> {
                        ModConfig.getInstance().toggleFavorite(home);
                        needsRebuild = true;
                    }
            ).bounds(panelX + PAD + mainBtnW + 4, btnY, 22, 20).build());
        }

        // Bottom buttons
        int bottomY = 20 + (height - 50) - 22;
        int bW = 52;
        int gap = 4;
        int totalW = bW * 4 + gap * 3;
        int startX = panelX + (PANEL_W - totalW) / 2;

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.refresh")),
                b -> {
                    HomesManager.getInstance().requestHomes();
                    allHomes.clear();
                    allHomes.addAll(HomesManager.getInstance().getHomes());
                    needsRebuild = true;
                }
        ).bounds(startX, bottomY, bW, 16).build());

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.recent")),
                b -> { if (minecraft != null) minecraft.setScreen(new HistoryScreen(this)); }
        ).bounds(startX + bW + gap, bottomY, bW, 16).build());

        addRenderableWidget(Button.builder(
                Component.literal("Stats"),
                b -> { if (minecraft != null) minecraft.setScreen(new StatsScreen(this)); }
        ).bounds(startX + (bW + gap) * 2, bottomY, bW, 16).build());

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.close")),
                b -> { if (minecraft != null) minecraft.setScreen(null); }
        ).bounds(startX + (bW + gap) * 3, bottomY, bW, 16).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 20;
        int panelH = height - 50;

        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, COLOR_PANEL);
        drawBorder(ctx, panelX, panelY, PANEL_W, panelH, COLOR_BORDER);

        ctx.drawCenteredString(font,
                Component.literal("✦ " + LangManager.getInstance().get("title.homes") + " ✦"),
                width / 2, panelY + 8, COLOR_ACCENT);

        if (filtered.isEmpty()) {
            String msg = allHomes.isEmpty()
                    ? LangManager.getInstance().get("message.no_homes")
                    : LangManager.getInstance().get("message.no_results");
            ctx.drawCenteredString(font, Component.literal("§7" + msg),
                    width / 2, panelY + 100, COLOR_DIM);
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
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
