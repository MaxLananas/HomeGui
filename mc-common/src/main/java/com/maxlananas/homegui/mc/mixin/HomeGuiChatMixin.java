package com.maxlananas.homegui.mc.mixin;

import com.maxlananas.homegui.mc.HomeGuiRuntime;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observes received chat so a {@code /homes} reply can be recognised.
 *
 * <p>Read only: the message is never modified or cancelled, and nothing is fed to
 * the parser unless a request is actually in flight.
 */
@Mixin(ChatComponent.class)
public class HomeGuiChatMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void homegui$capture(Component message, CallbackInfo ci) {
        if (!HomeGuiRuntime.isReady() || message == null) return;
        HomeGuiRuntime.onChatLine(message.getString());
    }
}
