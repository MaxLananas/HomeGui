package com.example.homegui;

import com.example.homegui.screen.HomesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeGuiClient implements ClientModInitializer {
    
    public static final String MOD_ID = "homegui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Touche pour ouvrir le GUI (H par défaut)
    private static KeyBinding openGuiKey;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Home GUI Mod chargé!");
        
        // Enregistrer la touche
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.homegui.open_gui",        // Nom de la touche
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,               // Touche H par défaut
            "category.homegui.main"        // Catégorie
        ));
        
        // Écouter les appuis de touche
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                openHomesGui(client);
            }
        });
    }
    
    private void openHomesGui(MinecraftClient client) {
        if (client.player == null) return;
        
        // Demander la liste des homes au serveur
        HomesManager.getInstance().requestHomes();
        
        // Ouvrir le GUI
        client.setScreen(new HomesScreen());
    }
}