package com.example.homegui.screen;

import com.example.homegui.HomesManager;
import com.example.homegui.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.Click;
import net.minecraft.client.gui.screen.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HomesScreen extends Screen {

    private final List<String> homes = new ArrayList<>();
    private int hoveredIndex = -1;

    public HomesScreen() {
        super(Text.literal("Homes"));
    }

    @Override
    protected void init() {
        homes.clear();
        homes.addAll(HomesManager.getInstance().getHomes());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xCC000000);

        hoveredIndex = -1;
        int y = 60;

        for (int i = 0; i < homes.size(); i++) {
            String home = homes.get(i);

            boolean hovered = mouseX >= width / 2 - 100 &&
                    mouseX <= width / 2 + 100 &&
                    mouseY >= y &&
                    mouseY <= y + 20;

            if (hovered) hoveredIndex = i;

            context.fill(width / 2 - 100, y, width / 2 + 100, y + 20,
                    hovered ? 0xFF4444AA : 0xFF222244);

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
            String home = homes.get(hoveredIndex);

            if (click.button() == 0) {
                ModConfig.getInstance().incrementUseCount(home);
                ModConfig.getInstance().addToHistory(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }

            if (click.button() == 1) {
                ModConfig.getInstance().toggleFavorite(home);
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput input) {

        int keyCode = input.keyCode();

        if (keyCode >= 49 && keyCode <= 57) {
            int index = keyCode - 49;
            if (index < homes.size()) {
                String home = homes.get(index);
                ModConfig.getInstance().incrementUseCount(home);
                HomesManager.getInstance().teleportToHome(home);
                return true;
            }
        }

        if (keyCode == 256) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
