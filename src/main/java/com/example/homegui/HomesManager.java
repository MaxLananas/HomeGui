package com.maxlananas.homegui;

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
    private boolean isWaitingForResponse = false;
    private long lastRequestTime = 0;
    private static final long TIMEOUT_MS = 5000L;

    private static final Pattern[] HOME_PATTERNS = {
        Pattern.compile("(?i)homes?\\s*:\\s*(.+)"),
        Pattern.compile("(?i)vos\\s+homes?\\s*:\\s*(.+)"),
        Pattern.compile("(?i)\\[home\\].*?:\\s*(.+)"),
    };
    private static final Pattern BRACKET_PATTERN =
            Pattern.compile("\\[([^\\]]+)\\]");

    private HomesManager() {}

    public static HomesManager getInstance() {
        if (instance == null) instance = new HomesManager();
        return instance;
    }

    public void requestHomes() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            isWaitingForResponse = true;
            lastRequestTime = System.currentTimeMillis();
            client.player.connection.sendChat("/homes");
            LOGGER.info("[HomeGUI] Homes list loadings....");
        }
    }

    public void onChatMessage(String message) {
        if (!isWaitingForResponse) return;
        if (System.currentTimeMillis() - lastRequestTime > TIMEOUT_MS) {
            isWaitingForResponse = false;
            return;
        }
        String clean = message
                .replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "")
                .trim();
        if (parseHomesMessage(clean)) {
            isWaitingForResponse = false;
            LOGGER.info("[HomeGUI] Homes finds: {}", homes);
        }
    }

    private boolean parseHomesMessage(String message) {
        Matcher bm = BRACKET_PATTERN.matcher(message);
        List<String> found = new ArrayList<>();
        while (bm.find()) {
            String h = bm.group(1).trim();
            if (!h.isEmpty() && !h.equalsIgnoreCase("home")) found.add(h);
        }
        if (!found.isEmpty()) {
            homes.clear();
            homes.addAll(found);
            return true;
        }
        for (Pattern p : HOME_PATTERNS) {
            Matcher m = p.matcher(message);
            if (m.find()) {
                parseHomesList(m.group(1));
                return !homes.isEmpty();
            }
        }
        if (message.toLowerCase().contains("home")) {
            String[] parts = message.split("[:,]");
            if (parts.length > 1) {
                homes.clear();
                for (int i = 1; i < parts.length; i++) {
                    String h = parts[i].trim()
                            .replaceAll("[\\[\\](){}]", "").trim();
                    if (!h.isEmpty() && h.length() < 30) homes.add(h);
                }
                return !homes.isEmpty();
            }
        }
        return false;
    }

    private void parseHomesList(String list) {
        homes.clear();
        for (String part : list.split("[,|\\s]+")) {
            String h = part.trim().replaceAll("[\\[\\](){}]", "").trim();
            if (!h.isEmpty() && h.length() < 30) homes.add(h);
        }
    }

    public void teleportToHome(String homeName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.connection.sendChat("/home " + homeName);
            client.setScreen(null);
        }
    }

    public List<String> getHomes()     { return new ArrayList<>(homes); }
    public void addHome(String home)   { if (!homes.contains(home)) homes.add(home); }
    public void clearHomes()           { homes.clear(); }
    public boolean isWaiting()         { return isWaitingForResponse; }
}
