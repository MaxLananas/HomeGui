package com.example.homegui.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    
    private static ModConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("homegui.json");
    
    // Données
    private int themeIndex = 0;
    private boolean compactMode = false;
    private String language = "en";
    private Set<String> favorites = new HashSet<>();
    private Map<String, Integer> useCounts = new HashMap<>();
    private List<HistoryEntry> history = new ArrayList<>();
    private int totalTeleports = 0;
    
    private ModConfig() {
        load();
    }
    
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }
    
    // ═══════════════════════════════════════════
    // LANGUE
    // ═══════════════════════════════════════════
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String lang) {
        this.language = lang;
        save();
    }
    
    // ═══════════════════════════════════════════
    // THÈME
    // ═══════════════════════════════════════════
    
    public int getThemeIndex() {
        return themeIndex;
    }
    
    public void setThemeIndex(int index) {
        this.themeIndex = index;
        save();
    }
    
    // ═══════════════════════════════════════════
    // MODE COMPACT
    // ═══════════════════════════════════════════
    
    public boolean isCompactMode() {
        return compactMode;
    }
    
    public void setCompactMode(boolean compact) {
        this.compactMode = compact;
        save();
    }
    
    // ═══════════════════════════════════════════
    // FAVORIS
    // ═══════════════════════════════════════════
    
    public boolean isFavorite(String homeName) {
        return favorites.contains(homeName.toLowerCase());
    }
    
    public boolean toggleFavorite(String homeName) {
        String key = homeName.toLowerCase();
        if (favorites.contains(key)) {
            favorites.remove(key);
            save();
            return false;
        } else {
            favorites.add(key);
            save();
            return true;
        }
    }
    
    // ═══════════════════════════════════════════
    // STATISTIQUES
    // ═══════════════════════════════════════════
    
    public int getUseCount(String homeName) {
        return useCounts.getOrDefault(homeName.toLowerCase(), 0);
    }
    
    public void incrementUseCount(String homeName) {
        String key = homeName.toLowerCase();
        useCounts.put(key, useCounts.getOrDefault(key, 0) + 1);
        totalTeleports++;
        save();
    }
    
    public int getTotalTeleports() {
        return totalTeleports;
    }
    
    public Map<String, Integer> getAllUseCounts() {
        return new HashMap<>(useCounts);
    }
    
    // ═══════════════════════════════════════════
    // HISTORIQUE
    // ═══════════════════════════════════════════
    
    public void addToHistory(String homeName) {
        history.removeIf(e -> e.homeName.equalsIgnoreCase(homeName));
        history.add(0, new HistoryEntry(homeName, System.currentTimeMillis()));
        
        if (history.size() > 15) {
            history = new ArrayList<>(history.subList(0, 15));
        }
        save();
    }
    
    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }
    
    public void clearHistory() {
        history.clear();
        save();
    }
    
    // ═══════════════════════════════════════════
    // PERSISTENCE - AVEC MIGRATION
    // ═══════════════════════════════════════════
    
    private void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("themeIndex", themeIndex);
            json.addProperty("compactMode", compactMode);
            json.addProperty("language", language);
            json.addProperty("totalTeleports", totalTeleports);
            
            // Favoris
            JsonArray favArray = new JsonArray();
            for (String fav : favorites) {
                favArray.add(fav);
            }
            json.add("favorites", favArray);
            
            // Use counts
            JsonObject countsObj = new JsonObject();
            for (Map.Entry<String, Integer> entry : useCounts.entrySet()) {
                countsObj.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("useCounts", countsObj);
            
            // Historique (nouveau format)
            JsonArray historyArray = new JsonArray();
            for (HistoryEntry entry : history) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("homeName", entry.homeName);
                entryObj.addProperty("timestamp", entry.timestamp);
                historyArray.add(entryObj);
            }
            json.add("history", historyArray);
            
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        
        try {
            String jsonString = Files.readString(CONFIG_PATH);
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            
            // Charger les valeurs simples
            if (json.has("themeIndex")) {
                themeIndex = json.get("themeIndex").getAsInt();
            }
            if (json.has("compactMode")) {
                compactMode = json.get("compactMode").getAsBoolean();
            }
            if (json.has("language")) {
                language = json.get("language").getAsString();
            }
            if (json.has("totalTeleports")) {
                totalTeleports = json.get("totalTeleports").getAsInt();
            }
            
            // Charger les favoris
            if (json.has("favorites")) {
                JsonArray favArray = json.getAsJsonArray("favorites");
                favorites.clear();
                for (JsonElement elem : favArray) {
                    favorites.add(elem.getAsString());
                }
            }
            
            // Charger les use counts
            if (json.has("useCounts")) {
                JsonObject countsObj = json.getAsJsonObject("useCounts");
                useCounts.clear();
                for (Map.Entry<String, JsonElement> entry : countsObj.entrySet()) {
                    useCounts.put(entry.getKey(), entry.getValue().getAsInt());
                }
            }
            
            // Charger l'historique - AVEC MIGRATION
            if (json.has("history")) {
                JsonArray historyArray = json.getAsJsonArray("history");
                history.clear();
                
                for (JsonElement elem : historyArray) {
                    try {
                        if (elem.isJsonObject()) {
                            // Nouveau format: {"homeName": "...", "timestamp": ...}
                            JsonObject entryObj = elem.getAsJsonObject();
                            String homeName = entryObj.get("homeName").getAsString();
                            long timestamp = entryObj.has("timestamp") ? 
                                entryObj.get("timestamp").getAsLong() : System.currentTimeMillis();
                            history.add(new HistoryEntry(homeName, timestamp));
                        } else if (elem.isJsonPrimitive()) {
                            // Ancien format: juste une string
                            String homeName = elem.getAsString();
                            history.add(new HistoryEntry(homeName, System.currentTimeMillis()));
                        }
                    } catch (Exception e) {
                        // Ignorer les entrées invalides
                    }
                }
            }
            
        } catch (Exception e) {
            // Si erreur de parsing, reset la config
            System.err.println("[HomeGUI] Error loading config, resetting: " + e.getMessage());
            resetToDefaults();
        }
    }
    
    private void resetToDefaults() {
        themeIndex = 0;
        compactMode = false;
        language = "en";
        favorites.clear();
        useCounts.clear();
        history.clear();
        totalTeleports = 0;
        
        // Supprimer le fichier corrompu et sauvegarder
        try {
            Files.deleteIfExists(CONFIG_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        save();
    }
    
    // ═══════════════════════════════════════════
    // CLASSE HISTORY ENTRY
    // ═══════════════════════════════════════════
    
    public static class HistoryEntry {
        public String homeName;
        public long timestamp;
        
        public HistoryEntry() {
            this.homeName = "";
            this.timestamp = System.currentTimeMillis();
        }
        
        public HistoryEntry(String homeName, long timestamp) {
            this.homeName = homeName;
            this.timestamp = timestamp;
        }
        
        public String getTimeAgo() {
            long diff = System.currentTimeMillis() - timestamp;
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) return days + "d";
            if (hours > 0) return hours + "h";
            if (minutes > 0) return minutes + "m";
            return seconds + "s";
        }
    }
}