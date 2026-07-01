package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class StatsScreen extends Screen {

    private static final int COLOR_BG     = 0xEE0A0A1A;
    private static final int COLOR_PANEL  = 0xDD121228;
    private static final int COLOR_ACCENT = 0xFF5B5BFF;
    private static final int COLOR_GOLD   = 0xFFFFD700;
    private static final int COLOR_SILVER = 0xFFAAAAAA;
    private static final int COLOR_BRONZE = 0xFFCD7F32;
    private static final int COLOR_TEXT   = 0xFFE0E0FF;
    private static final int COLOR_DIM    = 0xFF8888AA;
    private static final int COLOR_BORDER = 0xFF3A3A7A;

    private final Screen parent;

    public StatsScreen(Screen parent) {
        super(Component.literal("Statistics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelX = width / 2 - 150;
        int panelW = 300;
        int panelH = height - 40;
        int backY  = 15 + panelH - 22;
        int backW  = 90;
        int backX  = panelX + (panelW - backW) / 2;

        addRenderableWidget(Button.builder(
                Component.literal(LangManager.getInstance().get("button.back")),
                b -> { if (minecraft != null) minecraft.setScreen(parent); }
        ).bounds(backX, backY, backW, 16).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - 150;
        int panelW = 300;
        int panelY = 15;
        int panelH = height - 40;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(ctx, panelX, panelY, panelW, panelH, COLOR_BORDER);

        ctx.drawCenteredString(font,
                Component.literal("📊 " + LangManager.getInstance().get("title.stats")),
                width / 2, panelY + 8, COLOR_ACCENT);

        ModConfig config = ModConfig.getInstance();
        List<String> homes = HomesManager.getInstance().getHomes();
        LangManager lang   = LangManager.getInstance();

        int y     = panelY + 26;
        int cardW = (panelW - 36) / 3;
        int cardH = 36;

        drawCard(ctx, panelX + 12, y, cardW, cardH,
                String.valueOf(homes.size()), lang.get("stats.total_homes"), COLOR_ACCENT);
        drawCard(ctx, panelX + 12 + cardW + 6, y, cardW, cardH,
                String.valueOf(homes.stream().filter(config::isFavorite).count()),
                lang.get("stats.favorites"), COLOR_GOLD);
        drawCard(ctx, panelX + 12 + (cardW + 6) * 2, y, cardW, cardH,
                String.valueOf(config.getTotalTeleports()),
                lang.get("stats.total_tp"), 0xFF44FF88);

        y += cardH + 16;
        ctx.fill(panelX + 20, y, panelX + panelW - 20, y + 1, COLOR_BORDER);
        ctx.drawCenteredString(font,
                Component.literal("§8" + lang.get("stats.top_homes")),
                width / 2, y + 4, COLOR_DIM);
        y += 16;

        Map<String, Integer> counts = config.getAllUseCounts();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int maxCount = sorted.isEmpty() ? 1 : Math.max(1, sorted.get(0).getValue());
        int[] medals = {COLOR_GOLD, COLOR_SILVER, COLOR_BRONZE};

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int barW  = panelW - 36;
            int bX    = panelX + 18;
            int fillW = barW * entry.getValue() / maxCount;

            ctx.fill(bX, y, bX + barW, y + 16, 0xFF0F0F2A);
            int barColor = i < 3 ? medals[i] : 0xFF4A4AFF;
            ctx.fill(bX, y, bX + fillW, y + 16, (barColor & 0x55FFFFFF) | 0x55000000);
            ctx.fill(bX, y + 14, bX + fillW, y + 16, barColor);

            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "#" + (i + 1);
            ctx.drawString(font, Component.literal(medal), bX + 4, y + 4, COLOR_TEXT);
            ctx.drawCenteredString(font, Component.literal(entry.getKey()),
                    bX + barW / 2, y + 4, COLOR_TEXT);

            int v = entry.getValue();
            String visits = v + " " + (v > 1
                    ? lang.get("stats.visits_plural")
                    : lang.get("stats.visits"));
            ctx.drawString(font, Component.literal("§7" + visits),
                    bX + barW - font.width(visits) - 4, y + 4, COLOR_DIM);
            y += 20;
        }

        if (sorted.isEmpty()) {
            ctx.drawCenteredString(font,
                    Component.literal("§7Aucune donnée disponible"),
                    width / 2, y + 8, COLOR_DIM);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCard(GuiGraphics ctx, int x, int y, int w, int h,
                           String value, String label, int color) {
        ctx.fill(x, y, x + w, y + h, 0xFF0D0D24);
        ctx.fill(x, y, x + w, y + 2, color);
        drawBorder(ctx, x, y, w, h, COLOR_BORDER);
        ctx.drawCenteredString(font,
                Component.literal("§l" + value), x + w / 2, y + 8, color);
        ctx.drawCenteredString(font,
                Component.literal("§8" + label), x + w / 2, y + 22, COLOR_DIM);
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
