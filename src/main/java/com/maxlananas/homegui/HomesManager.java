package com.maxlananas.homegui;

import com.maxlananas.homegui.config.ModConfig;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("homegui");
    private static HomesManager instance;

    private final List<String> homes = new ArrayList<>();
    private boolean waiting = false;
    private long requestTime = 0;
    private static final long TIMEOUT = 5000L;

    // Matches a "home <name>" teleport command (with or without a leading slash),
    // but not the "homes" list command (which has no argument).
    private static final Pattern HOME_CMD = Pattern.compile("(?i)^/?home\\s+(\\S+)");

    private static final Pattern BRACKET = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern[] NAMED = {
            Pattern.compile("(?i)homes?\\s*:\\s*(.+)"),
            Pattern.compile("(?i)vos\\s+homes?\\s*:\\s*(.+)"),
            Pattern.compile("(?i)\\[home\\].*?:\\s*(.+)"),
    };

    private HomesManager() {}

    public static HomesManager getInstance() {
        if (instance == null) instance = new HomesManager();
        return instance;
    }

    public void requestHomes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            waiting = true;
            requestTime = System.currentTimeMillis();
            mc.player.connection.sendCommand("homes");
            LOGGER.info("[HomeGUI] Requesting homes…");
        }
    }

    public void teleportToHome(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("home " + name);
            mc.setScreen(null);
        }
    }

    /**
     * Called for every command the client sends. Tracks {@code /home <name>}
     * teleports (typed, macro'd, or triggered by the GUI) exactly once, so
     * stats and history reflect real usage regardless of how the teleport was
     * initiated.
     */
    public void onCommandSent(String command) {
        if (command == null) return;
        Matcher m = HOME_CMD.matcher(command.trim());
        if (!m.find()) return;
        String name = m.group(1);
        // Use the canonical casing from the known homes list when available.
        for (String h : homes) {
            if (h.equalsIgnoreCase(name)) { name = h; break; }
        }
        ModConfig cfg = ModConfig.getInstance();
        cfg.incrementUseCount(name);
        cfg.addToHistory(name);
        HomeGuiClient.scheduleCoordCapture(name);
    }

    public void onChatMessage(String message) {
        if (!waiting) return;
        if (System.currentTimeMillis() - requestTime > TIMEOUT) { waiting = false; return; }
        String clean = message.replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "").trim();
        if (parse(clean)) {
            waiting = false;
            LOGGER.info("[HomeGUI] Homes found: {}", homes);
        }
    }

    public List<String> getHomes()  { return new ArrayList<>(homes); }
    public boolean isWaiting()      { return waiting; }

    private boolean parse(String msg) {
        Matcher bm = BRACKET.matcher(msg);
        List<String> found = new ArrayList<>();
        while (bm.find()) {
            String h = bm.group(1).trim();
            if (!h.isEmpty() && !h.equalsIgnoreCase("home")) found.add(h);
        }
        if (!found.isEmpty()) { homes.clear(); homes.addAll(found); return true; }

        for (Pattern p : NAMED) {
            Matcher m = p.matcher(msg);
            if (m.find()) {
                homes.clear();
                for (String part : m.group(1).split("[,|\\s]+")) {
                    String h = part.replaceAll("[\\[\\](){}]", "").trim();
                    if (!h.isEmpty() && h.length() < 30) homes.add(h);
                }
                if (!homes.isEmpty()) return true;
            }
        }

        if (msg.toLowerCase().contains("home")) {
            String[] parts = msg.split("[:,]");
            if (parts.length > 1) {
                homes.clear();
                for (int i = 1; i < parts.length; i++) {
                    String h = parts[i].replaceAll("[\\[\\](){}]", "").trim();
                    if (!h.isEmpty() && h.length() < 30) homes.add(h);
                }
                return !homes.isEmpty();
            }
        }
        return false;
    }
}
