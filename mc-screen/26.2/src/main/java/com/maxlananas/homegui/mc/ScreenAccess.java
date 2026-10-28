package com.maxlananas.homegui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ScreenAccess {
    static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }
    static boolean isSingleplayer(Minecraft minecraft) { return minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished(); }
    static boolean hasTranslation(String key) { return net.minecraft.locale.Language.getInstance().has(key); }
}
