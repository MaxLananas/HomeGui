package com.maxlananas.homegui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ScreenAccess {
    static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }
}
