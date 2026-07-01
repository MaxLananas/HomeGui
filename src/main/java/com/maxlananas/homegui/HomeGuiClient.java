package com.maxlananas.homegui;

import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.screen.HomesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeGuiClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("homegui");

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        ModConfig.getInstance();
        LangManager.getInstance().loadFromConfig();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.homegui.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.homegui.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.player != null) {
                    HomesManager.getInstance().requestHomes();
                    client.setScreen(new HomesScreen());
                }
            }
        });

        LOGGER.info("[HomeGUI] Initialized for Minecraft 1.21.10 ✓");
    }
}
