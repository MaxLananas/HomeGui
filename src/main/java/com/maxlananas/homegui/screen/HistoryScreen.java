package com.maxlananas.homegui.screen;

import com.maxlananas.homegui.HomesManager;
import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HistoryScreen extends Screen {

    private static final int COLOR_BG     = 0xEE0A0A1A;
    private static final int COLOR_PANEL  = 0xDD121228;
    private static final int COLOR_ACCENT = 0xFF5B5BFF;
    private static final int COLOR_TEXT   = 0xFFE0E0FF;
    private static final int COLOR_DIM    = 0xFF8888AA;
    private static final int COLOR_BORDER = 0xFF3A3A7A;
    private static final int COLOR_BTN    = 0xFF1E1E3F;
    private static final int COLOR_ENTRY  = 0xFF1A1A3A;
    private static final int COLOR_HOVER  = 0xFF2A2A5A;

    private final Screen parent;
    private int hoveredIndex = -1;

    public HistoryScreen(Screen parent) {
        super(Component.literal("History"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_BG);

        int panelX = width / 2 - 130;
        int panelW = 260;
        int panelY = 20;
        int panelH = height - 50;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, COLOR_BORDER);
        ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, COLOR_BORDER);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, COLOR_BORDER);
        ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, COLOR_BORDER);

        String title = LangManager.getInstance().get("title.history");
        ctx.drawCenteredString(font,
                Component.literal("⟳ " + title), width / 2, panelY + 8, COLOR_ACCENT);

        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        hoveredIndex = -1;
        int y = panelY + 26;

        if (history.isEmpty()) {
            ctx.drawCenteredString(font,
                    Component.literal("§7" + LangManager.getInstance().get("message.no_history")),
                    width / 2, y + 20, COLOR_DIM);
        } else {
            for (int i = 0; i < history.size() && i < 12; i++) {
                ModConfig.HistoryEntry entry = history.get(i);
                int bX = panelX + 12;
                int bW = panelW - 24;
                int bH = 22;

                boolean hov = mouseX >= bX && mouseX <= bX + bW
                        && mouseY >= y && mouseY <= y + bH;
                if (hov) hoveredIndex = i;

                ctx.fill(bX, y, bX + bW, y + bH, hov ? COLOR_HOVER : COLOR_ENTRY);
                ctx.fill(bX, y, bX + bW, y + 1, hov ? COLOR_ACCENT : COLOR_BORDER);

                ctx.drawString(font,
                        Component.literal("§8" + (i + 1) + "."),
                        bX + 4, y + 7, COLOR_DIM);

                ctx.drawCenteredString(font,
                        Component.literal(entry.homeName),
                        bX + bW / 2, y + 7,
                        hov ? 0xFFFFFFFF : COLOR_TEXT);

                ctx.drawString(font,
                        Component.literal("§8" + entry.getTimeAgo()),
                        bX + bW - 28, y + 7, COLOR_DIM);

                y += 26;
            }
        }

        int btnY   = panelY + panelH - 22;
        int bW     = 80;
        int bH     = 16;
        int clearX = panelX + (panelW / 2) - bW - 4;
        int backX  = panelX + (panelW / 2) + 4;

        boolean clearHov = mouseX >= clearX && mouseX <= clearX + bW
                && mouseY >= btnY && mouseY <= btnY + bH;
        ctx.fill(clearX, btnY, clearX + bW, btnY + bH,
                clearHov ? 0xFF3A1A1A : COLOR_BTN);
        ctx.fill(clearX, btnY, clearX + bW, btnY + 1,
                clearHov ? 0xFFAA4444 : COLOR_BORDER);
        ctx.drawCenteredString(font,
                Component.literal(LangManager.getInstance().get("button.clear")),
                clearX + bW / 2, btnY + 4,
                clearHov ? 0xFFFF6666 : COLOR_DIM);

        boolean backHov = mouseX >= backX && mouseX <= backX + bW
                && mouseY >= btnY && mouseY <= btnY + bH;
        ctx.fill(backX, btnY, backX + bW, btnY + bH,
                backHov ? 0xFF1E1E3F : COLOR_BTN);
        ctx.fill(backX, btnY, backX + bW, btnY + 1,
                backHov ? COLOR_ACCENT : COLOR_BORDER);
        ctx.drawCenteredString(font,
                Component.literal(LangManager.getInstance().get("button.back")),
                backX + bW / 2, btnY + 4,
                backHov ? 0xFFFFFFFF : COLOR_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        int panelX = width / 2 - 130;
        int panelW = 260;
        int panelY = 20;
        int panelH = height - 50;
        int btnY   = panelY + panelH - 22;
        int bW     = 80;
        int bH     = 16;
        int clearX = panelX + (panelW / 2) - bW - 4;
        int backX  = panelX + (panelW / 2) + 4;

        if (mx >= clearX && mx <= clearX + bW && my >= btnY && my <= btnY + bH) {
            ModConfig.getInstance().clearHistory();
            return true;
        }

        if (mx >= backX && mx <= backX + bW && my >= btnY && my <= btnY + bH) {
            assert minecraft != null;
            minecraft.setScreen(parent);
            return true;
        }

        if (hoveredIndex >= 0) {
            List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
            if (hoveredIndex < history.size()) {
                String home = history.get(hoveredIndex).homeName;
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            assert minecraft != null;
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
