package com.example.homegui;

import com.example.homegui.config.LangManager;
import com.example.homegui.config.ModConfig;
import com.example.homegui.screen.HomesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeGuiClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("homegui");
    public static final String MOD_ID = "homegui";

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Charger config + langue
        ModConfig.getInstance();
        LangManager.getInstance().loadFromConfig();

        // Enregistrement du keybind
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.homegui.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.homegui.main"
        ));

        // Tick client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    HomesManager.getInstance().requestHomes();
                    client.setScreen(new HomesScreen());
                }
            }
        });

        LOGGER.info("[HomeGUI] Initialisé pour Minecraft 1.21.10 ✓");
    }
}
