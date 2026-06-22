package com.example.homegui.screen;

import com.example.homegui.HomesManager;
import com.example.homegui.config.LangManager;
import com.example.homegui.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;

public class StatsScreen extends Screen {

    private static final int COLOR_BG      = 0xEE0A0A1A;
    private static final int COLOR_PANEL   = 0xDD121228;
    private static final int COLOR_ACCENT  = 0xFF5B5BFF;
    private static final int COLOR_GOLD    = 0xFFFFD700;
    private static final int COLOR_SILVER  = 0xFFAAAAAA;
    private static final int COLOR_BRONZE  = 0xFFCD7F32;
    private static final int COLOR_TEXT    = 0xFFE0E0FF;
    private static final int COLOR_DIM     = 0xFF8888AA;
    private static final int COLOR_BORDER  = 0xFF3A3A7A;
    private static final int COLOR_BTN     = 0xFF1E1E3F;
    private static final int COLOR_BAR_BG  = 0xFF0F0F2A;
    private static final int COLOR_BAR_FG  = 0xFF4A4AFF;

    private final Screen parent;

    public StatsScreen(Screen parent) {
        super(Text.literal("Statistics"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - 150;
        int panelW = 300;
        int panelY = 15;
        int panelH = height - 40;

        // Panneau
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, COLOR_BORDER);
        ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, COLOR_BORDER);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, COLOR_BORDER);
        ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, COLOR_BORDER);

        // Titre
        String title = LangManager.getInstance().get("title.stats");
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("📊 " + title), width / 2, panelY + 8, COLOR_ACCENT);

        ModConfig config = ModConfig.getInstance();
        List<String> homes = HomesManager.getInstance().getHomes();

        int y = panelY + 26;

        // ─── Cartes de stats ─────────────────────────────────
        int cardW = (panelW - 36) / 3;
        int cardH = 36;
        int[] cardX = {
            panelX + 12,
            panelX + 12 + cardW + 6,
            panelX + 12 + (cardW + 6) * 2
        };

        // Total homes
        drawStatCard(ctx, cardX[0], y, cardW, cardH,
                String.valueOf(homes.size()),
                LangManager.getInstance().get("stats.total_homes"),
                COLOR_ACCENT);

        // Favoris
        long favCount = homes.stream()
                .filter(config::isFavorite).count();
        drawStatCard(ctx, cardX[1], y, cardW, cardH,
                String.valueOf(favCount),
                LangManager.getInstance().get("stats.favorites"),
                COLOR_GOLD);

        // Total téléportations
        drawStatCard(ctx, cardX[2], y, cardW, cardH,
                String.valueOf(config.getTotalTeleports()),
                LangManager.getInstance().get("stats.total_tp"),
                0xFF44FF88);

        y += cardH + 16;

        // ─── Séparateur ───────────────────────────────────────
        ctx.fill(panelX + 20, y, panelX + panelW - 20, y + 1, COLOR_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§8" + LangManager.getInstance().get("stats.top_homes")),
                width / 2, y + 4, COLOR_DIM);
        y += 16;

        // ─── Top 5 homes ──────────────────────────────────────
        Map<String, Integer> counts = config.getAllUseCounts();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int maxCount = sorted.isEmpty() ? 1 :
                Math.max(1, sorted.get(0).getValue());
        int[] medals = { COLOR_GOLD, COLOR_SILVER, COLOR_BRONZE };

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int barW = panelW - 36;
            int bX   = panelX + 18;
            int fillW = barW * entry.getValue() / maxCount;

            // Fond de barre
            ctx.fill(bX, y, bX + barW, y + 16, COLOR_BAR_BG);

            // Remplissage proportionnel
            int barColor = i < 3 ? medals[i] : COLOR_BAR_FG;
            ctx.fill(bX, y, bX + fillW, y + 16, barColor & 0x55FFFFFF | 0x55000000);
            ctx.fill(bX, y + 14, bX + fillW, y + 16, barColor);

            // Médaille
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "  #" + (i + 1);
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(medal), bX + 4, y + 4, COLOR_TEXT);

            // Nom
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(entry.getKey()),
                    bX + barW / 2, y + 4, COLOR_TEXT);

            // Compteur
            int v = entry.getValue();
            String visits = v + " " + (v > 1
                    ? LangManager.getInstance().get("stats.visits_plural")
                    : LangManager.getInstance().get("stats.visits"));
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§7" + visits),
                    bX + barW - textRenderer.getWidth(visits) - 4, y + 4, COLOR_DIM);

            y += 20;
        }

        if (sorted.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§7Aucune donnée disponible"),
                    width / 2, y + 8, COLOR_DIM);
        }

        // ─── Bouton Retour ────────────────────────────────────
        int backY = panelY + panelH - 22;
        int backW = 90;
        int backX = panelX + (panelW - backW) / 2;
        boolean backHov = mouseX >= backX && mouseX <= backX + backW
                && mouseY >= backY && mouseY <= backY + 16;

        ctx.fill(backX, backY, backX + backW, backY + 16,
                backHov ? 0xFF2E2E5F : COLOR_BTN);
        ctx.fill(backX, backY, backX + backW, backY + 1,
                backHov ? COLOR_ACCENT : COLOR_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(LangManager.getInstance().get("button.back")),
                backX + backW / 2, backY + 4,
                backHov ? 0xFFFFFFFF : COLOR_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawStatCard(DrawContext ctx, int x, int y, int w, int h,
                               String value, String label, int accentColor) {
        ctx.fill(x, y, x + w, y + h, 0xFF0D0D24);
        ctx.fill(x, y, x + w, y + 2, accentColor);
        ctx.fill(x, y, x + 1, y + h, COLOR_BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§l" + value), x + w / 2, y + 8, accentColor);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§8" + label), x + w / 2, y + 22, COLOR_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        int panelX = width / 2 - 150;
        int panelW = 300;
        int panelY = 15;
        int panelH = height - 40;
        int backY  = panelY + panelH - 22;
        int backW  = 90;
        int backX  = panelX + (panelW - backW) / 2;

        if (mx >= backX && mx <= backX + backW
                && my >= backY && my <= backY + 16) {
            assert client != null;
            client.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            assert client != null;
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
