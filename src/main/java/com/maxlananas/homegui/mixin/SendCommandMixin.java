package com.maxlananas.homegui.mixin;

import com.maxlananas.homegui.HomesManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks every command the client sends so that manually typed (or macro'd)
 * {@code /home <name>} teleports are counted just like GUI teleports. Because
 * the GUI also teleports through {@code sendCommand}, all tracking is funneled
 * through this single choke point to avoid double counting.
 */
@Mixin(ClientPacketListener.class)
public class SendCommandMixin {

    @Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void homegui$onSendCommand(String command, CallbackInfo ci) {
        HomesManager.getInstance().onCommandSent(command);
    }
}
