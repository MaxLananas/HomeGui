package com.maxlananas.homegui.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {

    private static ModConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("homegui.json");
    }

    private int     themeIndex     = 0;
    private boolean compactMode    = false;
    private String  language       = "en";
    private int     totalTeleports = 0;

    private final Set<String>         favorites = new HashSet<>();
    private final Map<String,Integer> useCounts = new HashMap<>();
    private List<HistoryEntry>        history   = new ArrayList<>();

    private ModConfig() { load(); }

    public static ModConfig getInstance() {
        if (instance == null) instance = new ModConfig();
        return instance;
    }

    public String  getLanguage()           { return language; }
    public void    setLanguage(String l)   { language = l; save(); }
    public int     getThemeIndex()         { return themeIndex; }
    public void    setThemeIndex(int idx)  { themeIndex = idx; save(); }
    public boolean isCompactMode()         { return compactMode; }
    public void    setCompactMode(boolean c) { compactMode = c; save(); }

    public boolean isFavorite(String home) {
        return favorites.contains(home.toLowerCase());
    }

    public boolean toggleFavorite(String home) {
        String k = home.toLowerCase();
        if (favorites.remove(k)) { save(); return false; }
        favorites.add(k); save(); return true;
    }

    public int getUseCount(String home) {
        return useCounts.getOrDefault(home.toLowerCase(), 0);
    }

    public void incrementUseCount(String home) {
        String k = home.toLowerCase();
        useCounts.merge(k, 1, Integer::sum);
        totalTeleports++;
        save();
    }

    public int getTotalTeleports()             { return totalTeleports; }
    public Map<String,Integer> getAllUseCounts() { return new HashMap<>(useCounts); }

    public void addToHistory(String home) {
        history.removeIf(e -> e.homeName.equalsIgnoreCase(home));
        history.add(0, new HistoryEntry(home, System.currentTimeMillis()));
        if (history.size() > 15) history = new ArrayList<>(history.subList(0, 15));
        save();
    }

    public List<HistoryEntry> getHistory()  { return new ArrayList<>(history); }
    public void clearHistory()              { history.clear(); save(); }

    private void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("themeIndex",     themeIndex);
            json.addProperty("compactMode",    compactMode);
            json.addProperty("language",       language);
            json.addProperty("totalTeleports", totalTeleports);

            JsonArray favArr = new JsonArray();
            favorites.forEach(favArr::add);
            json.add("favorites", favArr);

            JsonObject countsObj = new JsonObject();
            useCounts.forEach(countsObj::addProperty);
            json.add("useCounts", countsObj);

            JsonArray histArr = new JsonArray();
            for (HistoryEntry e : history) {
                JsonObject o = new JsonObject();
                o.addProperty("homeName",  e.homeName);
                o.addProperty("timestamp", e.timestamp);
                histArr.add(o);
            }
            json.add("history", histArr);

            Files.writeString(getConfigPath(), GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;
        try {
            JsonObject json = JsonParser.parseString(
                    Files.readString(path)).getAsJsonObject();

            if (json.has("themeIndex"))     themeIndex     = json.get("themeIndex").getAsInt();
            if (json.has("compactMode"))    compactMode    = json.get("compactMode").getAsBoolean();
            if (json.has("language"))       language       = json.get("language").getAsString();
            if (json.has("totalTeleports")) totalTeleports = json.get("totalTeleports").getAsInt();

            if (json.has("favorites")) {
                favorites.clear();
                json.getAsJsonArray("favorites").forEach(e -> favorites.add(e.getAsString()));
            }
            if (json.has("useCounts")) {
                useCounts.clear();
                json.getAsJsonObject("useCounts").entrySet()
                    .forEach(e -> useCounts.put(e.getKey(), e.getValue().getAsInt()));
            }
            if (json.has("history")) {
                history.clear();
                for (JsonElement el : json.getAsJsonArray("history")) {
                    try {
                        if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            history.add(new HistoryEntry(
                                    o.get("homeName").getAsString(),
                                    o.has("timestamp")
                                        ? o.get("timestamp").getAsLong()
                                        : System.currentTimeMillis()
                            ));
                        } else if (el.isJsonPrimitive()) {
                            history.add(new HistoryEntry(
                                    el.getAsString(), System.currentTimeMillis()));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[HomeGUI] Lost config, reset. " + e.getMessage());
            resetToDefaults();
        }
    }

    private void resetToDefaults() {
        themeIndex = 0; compactMode = false; language = "en";
        totalTeleports = 0;
        favorites.clear(); useCounts.clear(); history.clear();
        try { Files.deleteIfExists(getConfigPath()); } catch (IOException ignored) {}
        save();
    }

    public static class HistoryEntry {
        public String homeName;
        public long   timestamp;

        public HistoryEntry() {
            this.homeName  = "";
            this.timestamp = System.currentTimeMillis();
        }

        public HistoryEntry(String homeName, long timestamp) {
            this.homeName  = homeName;
            this.timestamp = timestamp;
        }

        public String getTimeAgo() {
            long s = (System.currentTimeMillis() - timestamp) / 1000;
            if (s < 60)    return s + "s";
            if (s < 3600)  return (s / 60) + "m";
            if (s < 86400) return (s / 3600) + "h";
            return (s / 86400) + "d";
        }
    }
}
