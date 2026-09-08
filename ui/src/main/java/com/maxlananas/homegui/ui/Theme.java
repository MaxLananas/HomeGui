package com.maxlananas.homegui.ui;

/**
 * The colour and spacing vocabulary of the interface.
 *
 * <p>Four palettes ship with the mod and the setting actually switches between them.
 * Every structural colour goes through {@link #surface} so that transparent mode is
 * applied in exactly one place.
 */
public final class Theme {

    /** One complete colour scheme. */
    public static final class Palette {
        public final String name;
        public final int backdrop;
        public final int panel;
        public final int panelEdge;
        public final int card;
        public final int cardHover;
        public final int input;
        public final int accent;
        public final int accentSoft;
        public final int text;
        public final int textDim;
        public final int textFaint;
        public final int border;
        public final int success;
        public final int danger;
        public final int warning;
        public final int favourite;

        Palette(String name, int backdrop, int panel, int panelEdge, int card, int cardHover,
                int input, int accent, int accentSoft, int text, int textDim, int textFaint,
                int border, int success, int danger, int warning, int favourite) {
            this.name = name;
            this.backdrop = backdrop;
            this.panel = panel;
            this.panelEdge = panelEdge;
            this.card = card;
            this.cardHover = cardHover;
            this.input = input;
            this.accent = accent;
            this.accentSoft = accentSoft;
            this.text = text;
            this.textDim = textDim;
            this.textFaint = textFaint;
            this.border = border;
            this.success = success;
            this.danger = danger;
            this.warning = warning;
            this.favourite = favourite;
        }
    }

    private static final Palette[] PALETTES = {
            new Palette("theme.indigo",
                    0xE00A0A16, 0xFF12122A, 0xFF1C1C3E, 0xFF191938, 0xFF232350,
                    0xFF14142E, 0xFF6C5CE7, 0x336C5CE7, 0xFFE8E8FF, 0xFF9A9AC4, 0xFF5C5C86,
                    0xFF262652, 0xFF2ED9A0, 0xFFFF6B6B, 0xFFFFC24B, 0xFFFFD166),
            new Palette("theme.slate",
                    0xE00C0F12, 0xFF161B21, 0xFF1F262E, 0xFF1D242C, 0xFF28313B,
                    0xFF12171C, 0xFF4DA3FF, 0x334DA3FF, 0xFFE6EEF5, 0xFF93A4B3, 0xFF59697A,
                    0xFF263140, 0xFF3DD68C, 0xFFFF7A6B, 0xFFFFC24B, 0xFFFFD166),
            new Palette("theme.forest",
                    0xE007120E, 0xFF101E19, 0xFF182B23, 0xFF15271F, 0xFF1E382C,
                    0xFF0E1A15, 0xFF3FBF7F, 0x333FBF7F, 0xFFE2F5EA, 0xFF8FBBA4, 0xFF557F69,
                    0xFF1D362B, 0xFF4ADE80, 0xFFFF7A6B, 0xFFFFC24B, 0xFFFFD166),
            new Palette("theme.contrast",
                    0xF2000000, 0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFF262626,
                    0xFF000000, 0xFFFFFF00, 0x55FFFF00, 0xFFFFFFFF, 0xFFE0E0E0, 0xFFB0B0B0,
                    0xFFFFFFFF, 0xFF00FF99, 0xFFFF5555, 0xFFFFFF00, 0xFFFFFF00),
    };

    /** Alpha multiplier applied to structural fills when transparent mode is on. */
    private static final float TRANSPARENT_SURFACE = 0.55F;
    private static final float TRANSPARENT_BACKDROP = 0.18F;

    private Theme() {}

    public static int paletteCount() { return PALETTES.length; }

    public static Palette palette(int index) {
        return PALETTES[Math.floorMod(index, PALETTES.length)];
    }

    /** Language key of a palette, so the setting can be shown localised. */
    public static String paletteKey(int index) {
        return palette(index).name;
    }

    /** Scales the alpha channel of an ARGB colour. */
    public static int alpha(int argb, float factor) {
        int value = (int) (((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, factor)));
        return (Math.max(0, Math.min(255, value)) << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend between two opaque colours. */
    public static int mix(int from, int to, float t) {
        float clamped = Math.max(0F, Math.min(1F, t));
        int a = blend(from >>> 24, to >>> 24, clamped);
        int r = blend(from >> 16, to >> 16, clamped);
        int g = blend(from >> 8, to >> 8, clamped);
        int b = blend(from, to, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int from, int to, float t) {
        return Math.max(0, Math.min(255, Math.round(((from & 0xFF) * (1 - t)) + ((to & 0xFF) * t))));
    }

    /** Structural fill colour, dimmed when the interface is set to transparent. */
    public static int surface(int argb, boolean transparent) {
        return transparent ? alpha(argb, TRANSPARENT_SURFACE) : argb;
    }

    /** Full screen backdrop, strongly dimmed in transparent mode so the world shows. */
    public static int backdrop(Palette palette, boolean transparent) {
        return transparent ? alpha(palette.backdrop, TRANSPARENT_BACKDROP) : palette.backdrop;
    }
}
