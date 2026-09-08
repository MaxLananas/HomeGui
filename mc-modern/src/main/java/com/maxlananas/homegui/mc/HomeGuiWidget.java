package com.maxlananas.homegui.mc;

import com.maxlananas.homegui.ui.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * One interface control, as a real vanilla widget.
 *
 * <p>Using a vanilla widget rather than a hand rolled hit area is what gives every
 * control working hover, focus, keyboard activation and screen reader support for
 * free. Everything about how it looks is delegated to the shared renderer, and the
 * widget's message is the control's full narration string, so the narrator describes
 * the same thing a sighted player sees.
 */
public final class HomeGuiWidget extends AbstractWidget {

    private final HomeGuiRuntime runtime;
    private final UiElement element;

    public HomeGuiWidget(HomeGuiRuntime runtime, UiElement element) {
        super(element.x, element.y, element.width, element.height,
                Component.literal(runtime.ui().narrationFor(element.id)));
        this.runtime = runtime;
        this.element = element;
    }

    public String elementId() {
        return element.id;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        runtime.ui().activate(element.id);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.ui().paintElement(runtime.painter(graphics), element, isHovered());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
