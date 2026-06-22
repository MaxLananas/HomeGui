package com.example.homegui.screen;

import com.example.homegui.HomesManager;
import com.example.homegui.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.Click;
import net.minecraft.client.gui.screen.KeyInput;
import net.minecraft.text.Text;

import java.util.List;

public class HistoryScreen extends Screen {

    private final Screen parent;
    private int hoveredIndex = -1;

    public HistoryScreen(Screen parent) {
        super(Text.literal("History"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xCC000000);

        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();

        hoveredIndex = -1;
        int y = 60;

        for (int i = 0; i < history.size(); i++) {
            String home = history.get(i).homeName;

            boolean hovered = mouseX >= width / 2 - 100 &&
                    mouseX <= width / 2 + 100 &&
                    mouseY >= y &&
                    mouseY <= y + 20;

            if (hovered) hoveredIndex = i;

            context.fill(width / 2 - 100, y, width / 2 + 100, y + 20,
                    hovered ? 0xFF884444 : 0xFF442222);

            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(home),
                    width / 2,
                    y + 6,
                    0xFFFFFF);

            y += 25;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (hoveredIndex >= 0) {
            List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
            if (hoveredIndex < history.size()) {
                String home = history.get(hoveredIndex).homeName;
                ModConfig.getInstance().incrementUseCount(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
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
