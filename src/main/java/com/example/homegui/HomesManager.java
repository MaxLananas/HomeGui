package com.example.homegui;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomesManager {

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

    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    private HomesManager() {}

    public static HomesManager getInstance() {
        if (instance == null) {
            instance = new HomesManager();
        }
        return instance;
    }

    public void requestHomes() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isWaitingForResponse = true;
            lastRequestTime = System.currentTimeMillis();
            client.player.networkHandler.sendChatCommand("homes");
            HomeGuiClient.LOGGER.info("[HomeGUI] Demande de la liste des homes...");
        }
    }

    public void onChatMessage(String message) {
        if (!isWaitingForResponse) return;
        if (System.currentTimeMillis() - lastRequestTime > TIMEOUT_MS) {
            isWaitingForResponse = false;
            return;
        }

        String clean = message.replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "").trim();

        if (parseHomesMessage(clean)) {
            isWaitingForResponse = false;
            HomeGuiClient.LOGGER.info("[HomeGUI] Homes trouvés: {}", homes);
        }
    }

    private boolean parseHomesMessage(String message) {
        // Homes entre crochets [home1] [home2]
        Matcher bracketMatcher = BRACKET_PATTERN.matcher(message);
        List<String> found = new ArrayList<>();
        while (bracketMatcher.find()) {
            String home = bracketMatcher.group(1).trim();
            if (!home.isEmpty() && !home.equalsIgnoreCase("home")) {
                found.add(home);
            }
        }
        if (!found.isEmpty()) {
            homes.clear();
            homes.addAll(found);
            return true;
        }

        // Patterns standards
        for (Pattern pattern : HOME_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                parseHomesList(matcher.group(1));
                return !homes.isEmpty();
            }
        }

        // Fallback : virgules après "home"
        if (message.toLowerCase().contains("home")) {
            String[] parts = message.split("[:,]");
            if (parts.length > 1) {
                homes.clear();
                for (int i = 1; i < parts.length; i++) {
                    String home = parts[i].trim().replaceAll("[\\[\\](){}]", "").trim();
                    if (!home.isEmpty() && home.length() < 30) {
                        homes.add(home);
                    }
                }
                return !homes.isEmpty();
            }
        }

        return false;
    }

    private void parseHomesList(String homesList) {
        homes.clear();
        String[] parts = homesList.split("[,|\\s]+");
        for (String part : parts) {
            String home = part.trim().replaceAll("[\\[\\](){}]", "").trim();
            if (!home.isEmpty() && home.length() < 30) {
                homes.add(home);
            }
        }
    }

    public void teleportToHome(String homeName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("home " + homeName);
            client.setScreen(null);
        }
    }

    public List<String> getHomes() {
        return new ArrayList<>(homes);
    }

    public void addHome(String home) {
        if (!homes.contains(home)) homes.add(home);
    }

    public void clearHomes() {
        homes.clear();
    }

    public boolean isWaiting() {
        return isWaitingForResponse;
    }
}
