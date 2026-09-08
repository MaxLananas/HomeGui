package com.maxlananas.homegui.mc;

import com.maxlananas.homegui.ui.HomeGuiUi;
import com.maxlananas.homegui.ui.UiElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Minecraft side of the interface, for the 1.21.9 and newer input API.
 *
 * <p>All layout, navigation and drawing decisions live in {@link HomeGuiUi}. This
 * class only translates between that model and this generation's screen callbacks,
 * and turns every {@link UiElement} into a {@link HomeGuiWidget} so that focus and
 * narration are handled by vanilla.
 */
public final class HomeGuiScreen extends Screen {

    private final HomeGuiRuntime runtime;
    private final Map<String, HomeGuiWidget> widgets = new LinkedHashMap<>();

    public HomeGuiScreen(HomeGuiRuntime runtime) {
        super(Component.literal("HomeGui"));
        this.runtime = runtime;
    }

    @Override
    protected void init() {
        runtime.ui().resize(width, height);
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        widgets.clear();
        HomeGuiUi ui = runtime.ui();
        ui.layout();
        for (UiElement element : ui.surface().elements()) {
            HomeGuiWidget widget = new HomeGuiWidget(runtime, element);
            widget.active = element.enabled;
            widgets.put(element.id, widget);
            addRenderableWidget(widget);
        }
        syncFocus();
    }

    private void syncFocus() {
        UiElement focused = runtime.ui().surface().focused();
        setFocused(focused == null ? null : widgets.get(focused.id));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (runtime.consumeInvalidation()) rebuild();
        HomeGuiUi ui = runtime.ui();
        ui.paintBackground(runtime.painter(graphics), width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
        ui.paintOverlay(runtime.painter(graphics), width, height);
    }

    @Override
    public void tick() {
        if (runtime.consumeInvalidation()) rebuild();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (matchesOpenKey(event)) {
            onClose();
            return true;
        }
        HomeGuiUi ui = runtime.ui();
        if (ui.keyPressed(event.key(), hasControl(event.modifiers()), hasShift(event.modifiers()))) {
            syncFocus();
            return true;
        }
        if (ui.searchKey(event.key(), hasControl(event.modifiers()))) return true;
        return super.keyPressed(event);
    }

    // GLFW modifier bits. Screen's hasControlDown()/hasShiftDown() helpers are gone in
    // this generation, and reading the bits off the event keeps the bridge off LWJGL.
    private static boolean hasControl(int modifiers) { return (modifiers & 0x0002) != 0; }

    private static boolean hasShift(int modifiers) { return (modifiers & 0x0001) != 0; }

    /** Pressing the (possibly rebound) open key again closes the interface. */
    private boolean matchesOpenKey(KeyEvent event) {
        Object key = runtime.openKey();
        return key instanceof KeyMapping && ((KeyMapping) key).matches(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return runtime.ui().charTyped((char) event.codepoint()) || super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        // The pressed state has to be recorded before the widgets handle the click, so
        // the frame that follows the press already shows it.
        int x = (int) event.x();
        int y = (int) event.y();
        runtime.ui().notePress(x, y, event.button());
        if (event.button() != 0 && runtime.ui().mouseClicked(x, y, event.button())) return true;
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return runtime.ui().mouseScrolled(vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        runtime.close();
    }

}
