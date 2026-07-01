package com.maxlananas.homegui.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Méthodes de dessin réutilisables pour toutes les screens.
 */
public final class UIRenderer {

    private UIRenderer() {}

    // ─────────────────────────────────────────────────────────────────────
    // RECTANGLES
    // ─────────────────────────────────────────────────────────────────────

    /** Panneau avec fond + bordure lumineuse sur le dessus */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        // Ombre extérieure
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        // Fond
        g.fill(x, y, x + w, y + h, UITheme.BG_PANEL);
        // Bordure
        drawBorder(g, x, y, w, h, UITheme.BORDER_NORMAL);
        // Ligne d'accent en haut
        g.fill(x + 1, y, x + w - 1, y + 1, UITheme.ACCENT_PRIMARY);
    }

    /** En-tête de panneau avec dégradé vers le bas */
    public static void drawHeader(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, UITheme.BG_ELEMENT);
        g.fill(x, y + h - 1, x + w, y + h, UITheme.BORDER_ACCENT);
    }

    /** Pied de page de panneau */
    public static void drawFooter(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, UITheme.BG_ELEMENT);
        g.fill(x, y, x + w, y + 1, UITheme.BORDER_ACCENT);
    }

    /** Rangée de liste avec état survol */
    public static void drawRow(GuiGraphics g, int x, int y, int w, int h,
                               boolean hovered, boolean favorite) {
        int bg = hovered ? UITheme.BG_HOVER : UITheme.BG_ELEMENT;
        g.fill(x, y, x + w, y + h, bg);
        if (hovered) {
            // Bordure gauche colorée au survol
            g.fill(x, y, x + 2, y + h, UITheme.ACCENT_PRIMARY);
        }
        if (favorite) {
            // Teinte dorée subtile pour les favoris
            g.fill(x, y, x + w, y + h, UITheme.COLOR_GOLD_DIM & 0x11FFFFFF | 0x0AFFD700);
            g.fill(x, y, x + 2, y + h,
                    hovered ? UITheme.COLOR_GOLD : 0xAAFFD700);
        }
        // Séparateur bas
        g.fill(x, y + h - 1, x + w, y + h, UITheme.BORDER_NORMAL);
    }

    /** Carte de statistique */
    public static void drawStatCard(GuiGraphics g, int x, int y, int w, int h,
                                    int accentColor) {
        g.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x44000000);
        g.fill(x, y, x + w, y + h, UITheme.BG_ELEMENT);
        drawBorder(g, x, y, w, h, UITheme.BORDER_NORMAL);
        // Barre supérieure colorée
        g.fill(x + 1, y, x + w - 1, y + 2, accentColor);
    }

    /** Barre de progression */
    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h,
                                       float ratio, int color) {
        // Fond
        g.fill(x, y, x + w, y + h, UITheme.BG_ELEMENT);
        // Remplissage
        int fillW = (int) (w * Math.max(0f, Math.min(1f, ratio)));
        if (fillW > 0) {
            // Couleur de remplissage semi-transparente
            g.fill(x, y, x + fillW, y + h,
                    (color & 0x00FFFFFF) | 0x33000000 | (color & 0xFF000000));
            // Ligne de contour au sommet
            g.fill(x, y, x + fillW, y + 1, color);
        }
        drawBorder(g, x, y, w, h, UITheme.BORDER_NORMAL);
    }

    /** Scrollbar verticale */
    public static void drawScrollbar(GuiGraphics g,
                                     int x, int y, int h,
                                     int scrollOffset, int maxScroll) {
        if (maxScroll <= 0) return;
        int w = UITheme.SCROLLBAR_W;
        g.fill(x, y, x + w, y + h, UITheme.BG_ELEMENT);
        float ratio    = (float) scrollOffset / maxScroll;
        int   thumbH   = Math.max(20, h / 3);
        int   thumbY   = y + (int) ((h - thumbH) * ratio);
        g.fill(x, thumbY, x + w, thumbY + thumbH, UITheme.BORDER_ACCENT);
    }

    /** Séparateur horizontal */
    public static void drawSeparator(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, UITheme.BORDER_NORMAL);
    }

    /** Fond d'écran global avec vignette */
    public static void drawBackground(GuiGraphics g, int screenW, int screenH) {
        // Fond plein
        g.fill(0, 0, screenW, screenH, UITheme.BG_OVERLAY);
        // Vignette : coins sombres
        int vSize = Math.min(screenW, screenH) / 3;
        for (int i = 0; i < vSize; i++) {
            int alpha = (int) (80 * (1f - (float) i / vSize));
            int col   = (alpha << 24);
            g.fill(0, 0, screenW - i, 1, col);           // haut
            g.fill(0, screenH - 1 - i, screenW, screenH - i, col); // bas
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BORDURES
    // ─────────────────────────────────────────────────────────────────────

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    public static void drawBorderGlow(GuiGraphics g, int x, int y, int w, int h) {
        // Bordure lumineuse à 2 pixels
        drawBorder(g, x - 1, y - 1, w + 2, h + 2, 0x22_6C6CFF);
        drawBorder(g, x,     y,     w,     h,      UITheme.BORDER_ACCENT);
    }

    // ─────────────────────────────────────────────────────────────────────
    // TEXTE
    // ─────────────────────────────────────────────────────────────────────

    /** Titre centré avec ombre */
    public static void drawTitle(GuiGraphics g, Font font,
                                 String text, int cx, int y, int color) {
        g.drawString(font, Component.literal(text),
                cx - font.width(text) / 2 + 1, y + 1, 0x44000000, false);
        g.drawString(font, Component.literal(text),
                cx - font.width(text) / 2, y, color, false);
    }

    /** Texte avec ombre portée */
    public static void drawShadowText(GuiGraphics g, Font font,
                                      String text, int x, int y, int color) {
        g.drawString(font, Component.literal(text), x + 1, y + 1,
                0x44000000, false);
        g.drawString(font, Component.literal(text), x, y, color, false);
    }

    /** Badge coloré (petit rectangle avec texte centré) */
    public static void drawBadge(GuiGraphics g, Font font,
                                 String text, int cx, int y, int color) {
        int tw = font.width(text);
        int bx = cx - tw / 2 - 3;
        int bw = tw + 6;
        g.fill(bx, y - 1, bx + bw, y + 9, (color & 0x00FFFFFF) | 0x33000000);
        drawBorder(g, bx, y - 1, bw, 10, color);
        g.drawString(font, Component.literal(text), bx + 3, y, color, false);
    }
}
