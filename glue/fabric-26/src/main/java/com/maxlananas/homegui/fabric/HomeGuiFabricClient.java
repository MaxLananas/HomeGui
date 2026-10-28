package com.maxlananas.homegui.fabric;

import com.maxlananas.homegui.mc.HomeGuiRuntime;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
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

        KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.homegui.open_gui",
                InputConstants.Type.KEYSYM,
                72,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("homegui", "category.main"))));
        runtime.setOpenKey(openKey);

        // Use Fabric's supported events instead of injecting into ChatComponent:
        // its addMessage signature changed in 26.2. Do not treat action-bar
        // overlays as chat replies.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) HomeGuiRuntime.onChatLine(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, type, time) ->
                HomeGuiRuntime.onChatLine(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runtime.tick(client);
            while (openKey.consumeClick()) runtime.open(client);
        });

        LOGGER.info("HomeGui ready (config: {})", runtime.config().configPath());
    }
}
