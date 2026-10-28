package com.maxlananas.homegui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ScreenAccess {
    static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }
    static boolean isSingleplayer(Minecraft minecraft) { return minecraft.isSingleplayer(); }
    static boolean hasTranslation(String key) { return net.minecraft.client.resources.language.I18n.exists(key); }
}
