package com.maxlananas.homegui;

import com.maxlananas.homegui.config.ModConfig;
import com.maxlananas.homegui.core.HomeCommand;
import com.maxlananas.homegui.core.HomeListParser;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class HomesManager {
    private static final long TIMEOUT_MILLIS = 5_000;
    private static final HomesManager INSTANCE = new HomesManager();

    private final HomeListParser parser = new HomeListParser();
    private List<String> homes = List.of();
    private boolean waiting;
    private long requestDeadline;
    private long revision;

    private HomesManager() {}

    public static HomesManager getInstance() { return INSTANCE; }

    public void requestHomes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) return;
        waiting = true;
        requestDeadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        minecraft.player.connection.sendCommand("homes");
    }

    public boolean teleportToHome(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) return false;
        var command = HomeCommand.teleport(name);
        if (command.isEmpty()) {
            HomeGuiClient.LOGGER.warn("Rejected an unsafe home name received from chat");
            return false;
        }
        minecraft.player.connection.sendCommand(command.get());
        minecraft.setScreen(null);
        return true;
    }

    public void onCommandSent(String command) {
        HomeCommand.teleportTarget(command).ifPresent(target -> {
            String canonical = homes.stream().filter(home -> home.equalsIgnoreCase(target)).findFirst().orElse(target);
            ModConfig.getInstance().recordTeleport(canonical, System.currentTimeMillis());
            HomeGuiClient.scheduleCoordCapture(canonical);
        });
    }

    public void onChatMessage(String message) {
        if (!waiting) return;
        if (System.currentTimeMillis() > requestDeadline) {
            waiting = false;
            revision++;
            return;
        }
        parser.parse(message).ifPresent(parsed -> {
            homes = parsed;
            waiting = false;
            revision++;
        });
    }

    public void tick() {
        if (waiting && System.currentTimeMillis() > requestDeadline) {
            waiting = false;
            revision++;
        }
    }

    public List<String> getHomes() { return homes; }
    public boolean isWaiting() { return waiting; }
    public long getRevision() { return revision; }
}
