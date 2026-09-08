package com.maxlananas.homegui.fabric;

import com.maxlananas.homegui.mc.HomeGuiRuntime;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric entrypoint for the 1.21.9 and newer generations.
 *
 * <p>Everything this class does is loader specific: where the configuration
 * directory is, how a keybind is registered, and how a client tick is observed. The
 * interface, the parser and the storage do not know Fabric exists.
 */
public final class HomeGuiFabricClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("homegui");

    @Override
    public void onInitializeClient() {
        HomeGuiRuntime.initialize(FabricLoader.getInstance().getConfigDir());
        HomeGuiRuntime runtime = HomeGuiRuntime.get();

        KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.homegui.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("homegui", "category.main"))));
        runtime.setOpenKey(openKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runtime.tick(client);
            while (openKey.consumeClick()) runtime.open(client);
        });

        LOGGER.info("HomeGui ready (config: {})", runtime.config().configPath());
    }
}
