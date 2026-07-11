package com.maxlananas.homegui.widget;

import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Theme {
    private Theme() {}

    // Alpha multipliers applied to structural fills when transparent menu mode is on.
    private static final float PANEL_ALPHA    = 0.62f;
    private static final float BACKDROP_ALPHA = 0.20f;

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

    private static boolean transparent() {
        return ModConfig.getInstance().isTransparentMenu();
    }

    /** Multiplies the alpha channel of an ARGB color by {@code mult} (clamped to 0-255). */
    public static int withAlpha(int argb, float mult) {
        int a = Math.round(((argb >>> 24) & 0xFF) * mult);
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Structural fill color (panels, cards, buttons, borders), dimmed in transparent mode. */
    public static int bg(int argb) {
        return transparent() ? withAlpha(argb, PANEL_ALPHA) : argb;
    }

    /** Full-screen backdrop color, strongly dimmed in transparent mode so the world shows through. */
    public static int backdrop() {
        return transparent() ? withAlpha(BG, BACKDROP_ALPHA) : BG;
    }

    public static void fillBorder(GuiGraphics g, int x, int y, int w, int h, int c) {
        c = bg(c);
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, bg(ACCENT_GLOW));
        g.fill(x, y, x + w, y + h, bg(PANEL));
        fillBorder(g, x, y, w, h, BORDER);
        g.fill(x + 1, y, x + w - 1, y + 2, bg(ACCENT));
    }

    public static void drawCard(GuiGraphics g, int x, int y, int w, int h, int accentColor) {
        g.fill(x, y, x + w, y + h, bg(CARD));
        g.fill(x, y, x + w, y + 2, bg(accentColor));
        fillBorder(g, x, y, w, h, BORDER);
    }

    public static void drawSeparator(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, bg(BORDER));
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
