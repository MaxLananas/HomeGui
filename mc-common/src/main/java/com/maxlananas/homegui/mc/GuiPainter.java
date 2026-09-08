package com.maxlananas.homegui.mc;

import com.maxlananas.homegui.ui.Painter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Draws the loader independent interface with the current version's GUI batcher.
 *
 * <p>Deliberately thin: the same handful of calls exists on every supported
 * Minecraft generation, which is why the interface itself never has to know which
 * one it is running on.
 */
public final class GuiPainter implements Painter {

    private final Font font;
    private GuiGraphics graphics;

    public GuiPainter(Font font) {
        this.font = font;
    }

    /** Rebinds to the frame's GUI batcher. Called once per render. */
    public GuiPainter bind(GuiGraphics graphics) {
        this.graphics = graphics;
        return this;
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        if (width <= 0 || height <= 0 || (argb >>> 24) == 0) return;
        graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        graphics.drawString(font, Component.literal(text), x, y, argb);
    }

    @Override
    public void centeredText(String text, int centerX, int y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        graphics.drawCenteredString(font, Component.literal(text), centerX, y, argb);
    }

    @Override
    public int textWidth(String text) {
        return text == null || text.isEmpty() ? 0 : font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }

    /**
     * Not implemented. The layout keeps every row inside the panel by construction,
     * so clipping is never required; leaving it out avoids depending on a scissor
     * API whose signature moved between generations.
     */
    @Override
    public void clip(int x, int y, int width, int height) {}

    @Override
    public void endClip() {}
}
