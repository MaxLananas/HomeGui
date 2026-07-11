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

    private static Path getExportPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("homegui-export.json");
    }

    private int     themeIndex     = 0;
    private boolean compactMode    = false;
    private boolean transparentMenu = false;
    private String  language       = "en";
    private String  sortMode       = "DEFAULT";
    private String  viewMode       = "list";
    private int     totalTeleports = 0;

    private final Set<String>                  favorites  = new HashSet<>();
    private final Map<String,Integer>          useCounts  = new HashMap<>();
    private final Map<String, HomeCoords>      homeCoords = new HashMap<>();
    private List<HistoryEntry>                 history    = new ArrayList<>();

    private ModConfig() { load(); }

    public static ModConfig getInstance() {
        if (instance == null) instance = new ModConfig();
        return instance;
    }

    public String  getLanguage()              { return language; }
    public void    setLanguage(String l)      { language = l; save(); }
    public int     getThemeIndex()            { return themeIndex; }
    public void    setThemeIndex(int i)       { themeIndex = i; save(); }
    public boolean isCompactMode()            { return compactMode; }
    public void    setCompactMode(boolean c)  { compactMode = c; save(); }
    public boolean isTransparentMenu()            { return transparentMenu; }
    public void    setTransparentMenu(boolean t)  { transparentMenu = t; save(); }
    public String  getSortMode()              { return sortMode; }
    public void    setSortMode(String s)      { sortMode = s; save(); }
    public String  getViewMode()              { return viewMode; }
    public void    setViewMode(String v)      { viewMode = v; save(); }

    public boolean isFavorite(String home)    { return favorites.contains(home.toLowerCase()); }

    public boolean toggleFavorite(String home) {
        String k = home.toLowerCase();
        if (favorites.remove(k)) { save(); return false; }
        favorites.add(k); save(); return true;
    }

    public int getUseCount(String home)       { return useCounts.getOrDefault(home.toLowerCase(), 0); }

    public void incrementUseCount(String home) {
        useCounts.merge(home.toLowerCase(), 1, Integer::sum);
        totalTeleports++;
        save();
    }

    public int  getTotalTeleports()            { return totalTeleports; }
    public Map<String,Integer> getAllUseCounts() { return new HashMap<>(useCounts); }

    public void setHomeCoords(String name, int x, int y, int z) {
        homeCoords.put(name.toLowerCase(), new HomeCoords(x, y, z));
        save();
    }

    public HomeCoords getHomeCoords(String name) {
        return homeCoords.get(name.toLowerCase());
    }

    public void addToHistory(String home) {
        history.removeIf(e -> e.homeName.equalsIgnoreCase(home));
        history.add(0, new HistoryEntry(home, System.currentTimeMillis()));
        if (history.size() > 15) history = new ArrayList<>(history.subList(0, 15));
        save();
    }

    public List<HistoryEntry> getHistory()    { return new ArrayList<>(history); }
    public void clearHistory()                { history.clear(); save(); }

    public void exportData() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("format", "homegui-v1");
            json.addProperty("exported_at", System.currentTimeMillis());

            JsonObject homes = new JsonObject();
            for (String fav : favorites) {
                JsonObject h = new JsonObject();
                h.addProperty("favorite", true);
                h.addProperty("use_count", getUseCount(fav));
                HomeCoords c = homeCoords.get(fav);
                if (c != null) {
                    JsonObject coord = new JsonObject();
                    coord.addProperty("x", c.x);
                    coord.addProperty("y", c.y);
                    coord.addProperty("z", c.z);
                    h.add("coords", coord);
                }
                homes.add(fav, h);
            }
            for (var entry : useCounts.entrySet()) {
                String k = entry.getKey();
                if (!homes.has(k)) {
                    JsonObject h = new JsonObject();
                    h.addProperty("favorite", false);
                    h.addProperty("use_count", entry.getValue());
                    HomeCoords c = homeCoords.get(k);
                    if (c != null) {
                        JsonObject coord = new JsonObject();
                        coord.addProperty("x", c.x);
                        coord.addProperty("y", c.y);
                        coord.addProperty("z", c.z);
                        h.add("coords", coord);
                    }
                    homes.add(k, h);
                }
            }
            json.add("homes", homes);

            JsonArray histArr = new JsonArray();
            for (HistoryEntry e : history) {
                JsonObject o = new JsonObject();
                o.addProperty("name", e.homeName);
                o.addProperty("timestamp", e.timestamp);
                histArr.add(o);
            }
            json.add("history", histArr);

            Files.writeString(getExportPath(), GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int importData() {
        Path path = getExportPath();
        if (!Files.exists(path)) return -1;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            int count = 0;

            if (json.has("homes")) {
                JsonObject homes = json.getAsJsonObject("homes");
                for (String name : homes.keySet()) {
                    JsonObject h = homes.getAsJsonObject(name);
                    if (h.has("favorite") && h.get("favorite").getAsBoolean()) {
                        favorites.add(name.toLowerCase());
                    }
                    if (h.has("use_count")) {
                        useCounts.merge(name.toLowerCase(), h.get("use_count").getAsInt(), Integer::sum);
                    }
                    if (h.has("coords")) {
                        JsonObject c = h.getAsJsonObject("coords");
                        homeCoords.put(name.toLowerCase(),
                                new HomeCoords(c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt()));
                    }
                    count++;
                }
            }

            if (json.has("history")) {
                for (JsonElement el : json.getAsJsonArray("history")) {
                    JsonObject o = el.getAsJsonObject();
                    history.removeIf(e -> e.homeName.equalsIgnoreCase(o.get("name").getAsString()));
                    history.add(new HistoryEntry(o.get("name").getAsString(),
                            o.has("timestamp") ? o.get("timestamp").getAsLong() : System.currentTimeMillis()));
                }
            }

            save();
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("themeIndex",     themeIndex);
            json.addProperty("compactMode",    compactMode);
            json.addProperty("transparentMenu", transparentMenu);
            json.addProperty("language",       language);
            json.addProperty("sortMode",       sortMode);
            json.addProperty("viewMode",       viewMode);
            json.addProperty("totalTeleports", totalTeleports);

            JsonArray favArr = new JsonArray();
            favorites.forEach(favArr::add);
            json.add("favorites", favArr);

            JsonObject countsObj = new JsonObject();
            useCounts.forEach(countsObj::addProperty);
            json.add("useCounts", countsObj);

            JsonObject coordsObj = new JsonObject();
            homeCoords.forEach((k, v) -> {
                JsonObject c = new JsonObject();
                c.addProperty("x", v.x);
                c.addProperty("y", v.y);
                c.addProperty("z", v.z);
                coordsObj.add(k, c);
            });
            json.add("homeCoords", coordsObj);

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
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (json.has("themeIndex"))     themeIndex     = json.get("themeIndex").getAsInt();
            if (json.has("compactMode"))    compactMode    = json.get("compactMode").getAsBoolean();
            if (json.has("transparentMenu")) transparentMenu = json.get("transparentMenu").getAsBoolean();
            if (json.has("language"))       language       = json.get("language").getAsString();
            if (json.has("sortMode"))       sortMode       = json.get("sortMode").getAsString();
            if (json.has("viewMode"))       viewMode       = json.get("viewMode").getAsString();
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
            if (json.has("homeCoords")) {
                homeCoords.clear();
                json.getAsJsonObject("homeCoords").entrySet().forEach(e -> {
                    JsonObject c = e.getValue().getAsJsonObject();
                    homeCoords.put(e.getKey(), new HomeCoords(
                            c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt()));
                });
            }
            if (json.has("history")) {
                history.clear();
                for (JsonElement el : json.getAsJsonArray("history")) {
                    try {
                        if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            history.add(new HistoryEntry(o.get("homeName").getAsString(),
                                    o.has("timestamp") ? o.get("timestamp").getAsLong() : System.currentTimeMillis()));
                        } else if (el.isJsonPrimitive()) {
                            history.add(new HistoryEntry(el.getAsString(), System.currentTimeMillis()));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[HomeGUI] Config corrupted, resetting. " + e.getMessage());
            resetDefaults();
        }
    }

    private void resetDefaults() {
        themeIndex = 0; compactMode = false; transparentMenu = false; language = "en";
        sortMode = "DEFAULT"; viewMode = "list"; totalTeleports = 0;
        favorites.clear(); useCounts.clear(); homeCoords.clear(); history.clear();
        try { Files.deleteIfExists(getConfigPath()); } catch (IOException ignored) {}
        save();
    }

    public static class HistoryEntry {
        public String homeName;
        public long   timestamp;
        public HistoryEntry()                      { this("", System.currentTimeMillis()); }
        public HistoryEntry(String name, long ts)  { this.homeName = name; this.timestamp = ts; }
        public String getTimeAgo() {
            long s = (System.currentTimeMillis() - timestamp) / 1000;
            if (s < 60)    return s + "s";
            if (s < 3600)  return (s / 60) + "m";
            if (s < 86400) return (s / 3600) + "h";
            return (s / 86400) + "d";
        }
    }

    public static class HomeCoords {
        public int x, y, z;
        public HomeCoords(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }
}
