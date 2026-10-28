package com.maxlananas.homegui.mc.mixin;

import com.maxlananas.homegui.mc.HomeGuiRuntime;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observes outgoing commands so that a {@code /home <name>} typed by hand, run from
 * a macro or sent by the interface is counted in exactly one place.
 *
 * <p>This is the single tracking point. Tracking the button instead would double
 * count GUI teleports, and counting here rather than on arrival is what keeps
 * statistics honest about where the command came from.
 */
@Mixin(ClientPacketListener.class)
public class HomeGuiCommandMixin {

    @Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void homegui$onSendCommand(String command, CallbackInfo ci) {
        if (!HomeGuiRuntime.isReady() || command == null) return;
        HomeGuiRuntime.onCommandSent(command);
    }
}
