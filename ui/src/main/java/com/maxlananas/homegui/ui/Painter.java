package com.maxlananas.homegui.ui;

/**
 * Everything the interface needs in order to draw itself.
 *
 * <p>Implemented once per Minecraft generation on top of that version's
 * {@code GuiGraphics}. Keeping the interface this small is what lets the whole
 * interface live in loader-independent code: nothing in here mentions a widget, a
 * matrix stack or a resource location.
 *
 * <p>All coordinates are GUI pixels, i.e. already scaled by the client's GUI scale.
 */
public interface Painter {

    /** Fills a rectangle. {@code argb} uses the standard Minecraft colour order. */
    void fill(int x, int y, int width, int height, int argb);

    /** Draws a one pixel border inside the given rectangle. */
    default void border(int x, int y, int width, int height, int argb) {
        fill(x, y, width, 1, argb);
        fill(x, y + height - 1, width, 1, argb);
        fill(x, y + 1, 1, height - 2, argb);
        fill(x + width - 1, y + 1, 1, height - 2, argb);
    }

    /** Draws left aligned text. */
    void text(String text, int x, int y, int argb, boolean shadow);

    /** Draws text centred on {@code centerX}. */
    void centeredText(String text, int centerX, int y, int argb, boolean shadow);

    /** Width of {@code text} in GUI pixels. */
    int textWidth(String text);

    /** Height of one line of text in GUI pixels. */
    int lineHeight();

    /** Restricts subsequent drawing to a rectangle. Must be paired with {@link #endClip()}. */
    void clip(int x, int y, int width, int height);

    void endClip();

    /** Shortens {@code text} so that it fits, adding an ellipsis when it does not. */
    default String truncate(String text, int maxWidth) {
        if (text == null || text.isEmpty() || textWidth(text) <= maxWidth) return text;
        String ellipsis = "\u2026";
        int reserved = textWidth(ellipsis);
        String current = text;
        while (current.length() > 1 && textWidth(current) + reserved > maxWidth) {
            current = current.substring(0, current.length() - 1);
        }
        return current + ellipsis;
    }
}
