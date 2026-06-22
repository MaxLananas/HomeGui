package com.example.homegui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.Click;
import net.minecraft.client.gui.screen.KeyInput;
import net.minecraft.text.Text;

public class StatsScreen extends Screen {

    private final Screen parent;

    public StatsScreen(Screen parent) {
        super(Text.literal("Stats"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xCC000000);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Statistics"),
                width / 2,
                50,
                0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        client.setScreen(parent);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.keyCode() == 256) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
