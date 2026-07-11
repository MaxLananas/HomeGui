package com.maxlananas.homegui;

import com.maxlananas.homegui.config.LangManager;
import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.screen.HomesScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeGuiClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("homegui");
    private static KeyMapping openGuiKey;
    private static String pendingCoordHome = null;
    private static int coordCaptureCountdown = 0;

    @Override
    public void onInitializeClient() {
        ModConfig.getInstance();
        LangManager.getInstance().loadFromConfig();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.homegui.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                new KeyMapping.Category(ResourceLocation.parse("homegui:category.main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.player != null) {
                    HomesManager.getInstance().requestHomes();
                    client.setScreen(new HomesScreen());
                }
            }
            if (coordCaptureCountdown > 0) {
                coordCaptureCountdown--;
                if (coordCaptureCountdown == 0 && client.player != null && pendingCoordHome != null) {
                    var pos = client.player.blockPosition();
                    ModConfig.getInstance().setHomeCoords(pendingCoordHome, pos.getX(), pos.getY(), pos.getZ());
                    pendingCoordHome = null;
                }
            }
        });

        LOGGER.info("[HomeGUI] Initialized ✓");
    }

    public static void scheduleCoordCapture(String homeName) {
        pendingCoordHome = homeName;
        coordCaptureCountdown = 20;
    }

    /** True if the given key event matches the (possibly rebound) "open GUI" keybind. */
    public static boolean matchesOpenKey(net.minecraft.client.input.KeyEvent event) {
        return openGuiKey != null && openGuiKey.matches(event);
    }
}
