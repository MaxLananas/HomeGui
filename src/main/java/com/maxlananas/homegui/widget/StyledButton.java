package com.maxlananas.homegui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class StyledButton extends AbstractWidget {

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

    public StyledButton(int x, int y, int w, int h, String label, Runnable onPress) {
        this(x, y, w, h, label, onPress,
             Theme.BTN, Theme.BTN_HOV, Theme.BORDER, Theme.TEXT, 0xFFFFFFFF);
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean doubled) {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        boolean hov = isHovered();
        int bx = getX(), by = getY(), bw = getWidth(), bh = getHeight();

        g.fill(bx, by, bx + bw, by + bh, Theme.bg(hov ? bgHover : bgNormal));
        g.fill(bx, by, bx + bw, by + 1, Theme.bg(hov ? borderAccent : Theme.BORDER));
        g.fill(bx, by + bh - 1, bx + bw, by + bh, Theme.bg(Theme.BORDER));
        g.fill(bx, by, bx + 1, by + bh, Theme.bg(Theme.BORDER));
        g.fill(bx + bw - 1, by, bx + bw, by + bh, Theme.bg(Theme.BORDER));

        Font font = Minecraft.getInstance().font;
        String raw = getMessage().getString();
        String label = Theme.truncate(font, raw, bw - 8);
        int color = hov ? textHover : textNormal;
        g.drawCenteredString(font, Component.literal(label),
                bx + bw / 2, by + (bh - 8) / 2, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
