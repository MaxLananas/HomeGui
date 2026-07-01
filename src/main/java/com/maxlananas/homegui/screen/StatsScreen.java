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

import java.util.*;

public class StatsScreen extends Screen {

    private static final int PANEL_W = 310;
    private static final int[] MEDALS = { Theme.GOLD, 0xFFAAAAAA, 0xFFCD7F32 };

    private final Screen parent;

    public StatsScreen(Screen parent) {
        super(Component.literal("Stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelX = width / 2 - PANEL_W / 2;
        int panelH = height - 40;
        int backW = 100;
        int backX = panelX + (PANEL_W - backW) / 2;
        int backY = 15 + panelH - 24;

        addRenderableWidget(new StyledButton(backX, backY, backW, 18,
                LangManager.getInstance().get("button.back"),
                () -> { if (minecraft != null) minecraft.setScreen(parent); }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        LangManager L = LangManager.getInstance();
        Font f = font;
        ModConfig cfg = ModConfig.getInstance();
        List<String> homes = HomesManager.getInstance().getHomes();

        g.fill(0, 0, width, height, Theme.BG);

        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 15;
        int panelH = height - 40;

        Theme.drawPanel(g, panelX, panelY, PANEL_W, panelH);
        Theme.drawTextCentered(g, f, "📊 " + L.get("title.stats"),
                width / 2, panelY + 10, Theme.ACCENT);

        // ── Stat cards ────────────────────────────
        int y = panelY + 30;
        int cw = (PANEL_W - 48) / 3;
        int ch = 40;
        int[] cx = { panelX + 12, panelX + 12 + cw + 12, panelX + 12 + (cw + 12) * 2 };

        drawStatCard(g, cx[0], y, cw, ch, String.valueOf(homes.size()),
                L.get("stats.total_homes"), Theme.ACCENT);
        drawStatCard(g, cx[1], y, cw, ch,
                String.valueOf(homes.stream().filter(cfg::isFavorite).count()),
                L.get("stats.favorites"), Theme.GOLD);
        drawStatCard(g, cx[2], y, cw, ch, String.valueOf(cfg.getTotalTeleports()),
                L.get("stats.total_tp"), Theme.SUCCESS);

        y += ch + 16;

        // ── Separator + section title ─────────────
        Theme.drawSeparator(g, panelX + 20, y, PANEL_W - 40);
        y += 6;
        Theme.drawTextCentered(g, f, "§8" + L.get("stats.top_homes"), width / 2, y, Theme.DIM);
        y += 14;

        // ── Bar chart ─────────────────────────────
        Map<String, Integer> counts = cfg.getAllUseCounts();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int maxVal = sorted.isEmpty() ? 1 : Math.max(1, sorted.get(0).getValue());
        int barW = PANEL_W - 48;
        int barX = panelX + 24;

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int fillW = barW * entry.getValue() / maxVal;
            int barColor = i < 3 ? MEDALS[i] : Theme.ACCENT_DIM;

            // Bar background
            g.fill(barX, y, barX + barW, y + 18, Theme.CARD);
            // Bar fill
            g.fill(barX, y, barX + fillW, y + 18, (barColor & 0x55FFFFFF) | 0x33000000);
            // Bottom accent line
            g.fill(barX, y + 16, barX + fillW, y + 18, barColor);

            // Medal / rank
            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "#" + (i + 1);
            };
            g.drawString(f, Component.literal(medal), barX + 4, y + 5, Theme.TEXT);

            // Name (centered)
            Theme.drawTextCentered(g, f, Theme.truncate(f, entry.getKey(), barW - 80),
                    barX + barW / 2, y + 5, Theme.TEXT);

            // Visit count (right)
            int v = entry.getValue();
            String visits = v + " " + (v > 1 ? L.get("stats.visits_plural") : L.get("stats.visits"));
            g.drawString(f, Component.literal("§8" + visits),
                    barX + barW - f.width(visits) - 4, y + 5, Theme.DIM);

            y += 22;
        }

        if (sorted.isEmpty()) {
            Theme.drawTextCentered(g, f, "§7Aucune donnée disponible", width / 2, y + 10, Theme.DIM);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    private void drawStatCard(GuiGraphics g, int x, int y, int w, int h,
                               String value, String label, int accent) {
        Theme.drawCard(g, x, y, w, h, accent);
        Theme.drawTextCentered(g, font, "§l" + value, x + w / 2, y + 10, accent);
        Theme.drawTextCentered(g, font, "§8" + label, x + w / 2, y + 24, Theme.DIM);
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
