package com.maxlananas.homegui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ClickableWidget;
import net.minecraft.client.gui.components.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A custom-styled button that renders with our dark theme
 * instead of Minecraft's default gray buttons.
 */
public class StyledButton extends ClickableWidget {

    private final Runnable onPress;
    private final int bgNormal;
    private final int bgHover;
    private final int borderAccent;
    private final int textNormal;
    private final int textHover;

    public StyledButton(int x, int y, int w, int h, String label, Runnable onPress,
                        int bgNormal, int bgHover,
                        int borderAccent, int textNormal, int textHover) {
        super(x, y, w, h, Component.literal(label));
        this.onPress = onPress;
        this.bgNormal = bgNormal;
        this.bgHover = bgHover;
        this.borderAccent = borderAccent;
        this.textNormal = textNormal;
        this.textHover = textHover;
    }

    /** Convenience: standard button with theme defaults. */
    public StyledButton(int x, int y, int w, int h, String label, Runnable onPress) {
        this(x, y, w, h, label, onPress,
             Theme.BTN, Theme.BTN_HOV, Theme.BORDER, Theme.TEXT, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        boolean hov = isHovered();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // Background
        g.fill(x, y, x + w, y + h, hov ? bgHover : bgNormal);

        // Top accent border
        g.fill(x, y, x + w, y + 1, hov ? borderAccent : Theme.BORDER);
        // Subtle remaining border
        g.fill(x, y + h - 1, x + w, y + h, Theme.BORDER);
        g.fill(x, y, x + 1, y + h, Theme.BORDER);
        g.fill(x + w - 1, y, x + w, y + h, Theme.BORDER);

        // Text (centered, truncated if needed)
        Font font = Minecraft.getInstance().font;
        String raw = getMessage().getString();
        String label = Theme.truncate(font, raw, w - 8);
        int color = hov ? textHover : textNormal;
        g.drawCenteredString(font, Component.literal(label),
                x + w / 2, y + (h - 8) / 2, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
