package com.maxlananas.homegui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Theme {
    private Theme() {}

    public static final int BG          = 0xFF080816;
    public static final int PANEL       = 0xFF0E0E24;
    public static final int CARD        = 0xFF151536;
    public static final int INPUT_BG    = 0xFF111130;

    public static final int ACCENT      = 0xFF6C5CE7;
    public static final int ACCENT_DIM  = 0xFF4834D4;
    public static final int ACCENT_GLOW = 0x306C5CE7;

    public static final int GOLD        = 0xFFFFD700;
    public static final int SUCCESS     = 0xFF00D68F;
    public static final int DANGER      = 0xFFFF6B6B;
    public static final int INFO        = 0xFF44AAFF;

    public static final int TEXT        = 0xFFE4E4FF;
    public static final int DIM         = 0xFF7878A0;
    public static final int FAINT       = 0xFF4A4A70;

    public static final int BORDER      = 0xFF222250;
    public static final int BORDER_L    = 0xFF333368;
    public static final int BTN         = 0xFF161638;
    public static final int BTN_HOV     = 0xFF222255;

    public static void fillBorder(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, ACCENT_GLOW);
        g.fill(x, y, x + w, y + h, PANEL);
        fillBorder(g, x, y, w, h, BORDER);
        g.fill(x + 1, y, x + w - 1, y + 2, ACCENT);
    }

    public static void drawCard(GuiGraphics g, int x, int y, int w, int h, int accentColor) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 2, accentColor);
        fillBorder(g, x, y, w, h, BORDER);
    }

    public static void drawSeparator(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BORDER);
    }

    public static void drawTextCentered(GuiGraphics g, Font font, String text, int cx, int y, int color) {
        g.drawCenteredString(font, Component.literal(text), cx, y, color);
    }

    public static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }
}
