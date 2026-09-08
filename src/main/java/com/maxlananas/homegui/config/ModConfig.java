package com.maxlananas.homegui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maxlananas.homegui.HomeGuiClient;
import com.maxlananas.homegui.core.HomeNames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModConfig {
    private static final int SCHEMA_VERSION = 2;
    private static final int MAX_HISTORY = 15;
    private static final int MAX_TRACKED_HOMES = 2_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig instance;
    private static Path configDirectory;

    private int themeIndex;
    private boolean compactMode;
    private boolean transparentMenu;
    private String language = "en";
    private String sortMode = "DEFAULT";
    private String viewMode = "list";
    private int totalTeleports;
    private final Set<String> favorites = new HashSet<>();
    private final Map<String, Integer> useCounts = new HashMap<>();
    private final Map<String, HomeCoords> homeCoords = new HashMap<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    private ModConfig() {
        load();
    }

    public static void initialize(Path directory) {
        if (configDirectory != null) return;
        configDirectory = directory.toAbsolutePath().normalize();
        instance = new ModConfig();
    }

    public static ModConfig getInstance() {
        if (instance == null) throw new IllegalStateException("HomeGUI config has not been initialized");
        return instance;
    }

    private static Path configPath() { return configDirectory.resolve("homegui.json"); }
    private static Path exportPath() { return configDirectory.resolve("homegui-export.json"); }

    public String getLanguage() { return language; }
    public int getThemeIndex() { return themeIndex; }
    public boolean isCompactMode() { return compactMode; }
    public boolean isTransparentMenu() { return transparentMenu; }
    public String getSortMode() { return sortMode; }
    public String getViewMode() { return viewMode; }
    public int getTotalTeleports() { return totalTeleports; }

    public void setLanguage(String value) {
        language = value != null && value.matches("[a-z]{2}(?:_[a-z]{2})?") ? value : "en";
        save();
    }

    public void setThemeIndex(int value) { themeIndex = Math.max(0, Math.min(4, value)); save(); }
    public void setCompactMode(boolean value) { compactMode = value; save(); }
    public void setTransparentMenu(boolean value) { transparentMenu = value; save(); }
    public void setSortMode(String value) { sortMode = SortMode.fromString(value).name(); save(); }
    public void setViewMode(String value) { viewMode = "grid".equals(value) ? "grid" : "list"; save(); }

    public boolean isFavorite(String home) { return home != null && favorites.contains(HomeNames.key(home)); }

    public boolean toggleFavorite(String home) {
        var valid = HomeNames.validate(home);
        if (valid.isEmpty()) return false;
        String key = HomeNames.key(valid.get());
        boolean selected = favorites.add(key);
        if (!selected) favorites.remove(key);
        save();
        return selected;
    }

    public int getUseCount(String home) {
        return home == null ? 0 : useCounts.getOrDefault(HomeNames.key(home), 0);
    }

    public void recordTeleport(String home, long timestamp) {
        var valid = HomeNames.validate(home);
        if (valid.isEmpty()) return;
        String displayName = valid.get();
        String key = HomeNames.key(displayName);
        if (useCounts.size() < MAX_TRACKED_HOMES || useCounts.containsKey(key)) {
            useCounts.compute(key, (ignored, count) -> count == null ? 1 : saturatedIncrement(count));
        }
        totalTeleports = saturatedIncrement(totalTeleports);
        history.removeIf(entry -> entry.homeName.equalsIgnoreCase(displayName));
        history.add(0, new HistoryEntry(displayName, saneTimestamp(timestamp)));
        trimHistory();
        save();
    }

    public Map<String, Integer> getAllUseCounts() { return Map.copyOf(useCounts); }

    public void setHomeCoords(String name, int x, int y, int z) {
        HomeNames.validate(name).ifPresent(valid -> {
            if (homeCoords.size() < MAX_TRACKED_HOMES || homeCoords.containsKey(HomeNames.key(valid))) {
                homeCoords.put(HomeNames.key(valid), new HomeCoords(x, y, z));
                save();
            }
        });
    }

    public HomeCoords getHomeCoords(String name) {
        return name == null ? null : homeCoords.get(HomeNames.key(name));
    }

    public List<HistoryEntry> getHistory() { return List.copyOf(history); }
    public void clearHistory() { history.clear(); save(); }

    public boolean exportData() {
        JsonObject root = toJson();
        root.addProperty("format", "homegui-v2");
        root.addProperty("exportedAt", System.currentTimeMillis());
        return writeAtomically(exportPath(), root);
    }

    public int importData() {
        Path path = exportPath();
        if (!Files.isRegularFile(path)) return -1;
        try {
            if (Files.size(path) > 2_000_000) return -1;
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            int imported = mergePortable(root);
            save();
            return imported;
        } catch (Exception exception) {
            HomeGuiClient.LOGGER.warn("Could not import {}: {}", path.getFileName(), exception.getMessage());
            return -1;
        }
    }

    private int mergePortable(JsonObject root) {
        int count = 0;
        if (root.has("favorites") && root.get("favorites").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("favorites")) {
                if (count >= MAX_TRACKED_HOMES) break;
                HomeNames.validate(stringValue(element)).ifPresent(name -> favorites.add(HomeNames.key(name)));
                count++;
            }
        }
        readCounts(root.get("useCounts"), true);
        readCoords(root.get("homeCoords"));
        readHistory(root.get("history"), true);

        // v1 exports represented every tracked home as an object under "homes".
        if (root.has("homes") && root.get("homes").isJsonObject()) {
            for (var entry : root.getAsJsonObject("homes").entrySet()) {
                if (count >= MAX_TRACKED_HOMES) break;
                var valid = HomeNames.validate(entry.getKey());
                if (valid.isEmpty() || !entry.getValue().isJsonObject()) continue;
                String key = HomeNames.key(valid.get());
                JsonObject data = entry.getValue().getAsJsonObject();
                if (booleanValue(data.get("favorite"), false)) favorites.add(key);
                int uses = boundedInt(data.get("use_count"), 0, 0, Integer.MAX_VALUE);
                if (uses > 0) useCounts.merge(key, uses, ModConfig::saturatedAdd);
                readCoordinate(key, data.get("coords"));
                count++;
            }
        }
        return count;
    }

    private void load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) return;
        try {
            if (Files.size(path) > 2_000_000) throw new IOException("file exceeds 2 MB");
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("root must be an object");
            read(parsed.getAsJsonObject());
        } catch (Exception exception) {
            preserveCorruptFile(path);
            resetDefaults();
            save();
            HomeGuiClient.LOGGER.warn("Invalid config was backed up and replaced: {}", exception.getMessage());
        }
    }

    private void read(JsonObject root) {
        themeIndex = boundedInt(root.get("themeIndex"), 0, 0, 4);
        compactMode = booleanValue(root.get("compactMode"), false);
        transparentMenu = booleanValue(root.get("transparentMenu"), false);
        String loadedLanguage = stringValue(root.get("language"));
        language = loadedLanguage.matches("[a-z]{2}(?:_[a-z]{2})?") ? loadedLanguage : "en";
        sortMode = SortMode.fromString(stringValue(root.get("sortMode"))).name();
        viewMode = "grid".equals(stringValue(root.get("viewMode"))) ? "grid" : "list";
        totalTeleports = boundedInt(root.get("totalTeleports"), 0, 0, Integer.MAX_VALUE);

        if (root.has("favorites") && root.get("favorites").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("favorites")) {
                if (favorites.size() >= MAX_TRACKED_HOMES) break;
                HomeNames.validate(stringValue(element)).ifPresent(name -> favorites.add(HomeNames.key(name)));
            }
        }
        readCounts(root.get("useCounts"), false);
        readCoords(root.get("homeCoords"));
        readHistory(root.get("history"), false);
    }

    private void readCounts(JsonElement element, boolean merge) {
        if (element == null || !element.isJsonObject()) return;
        for (var entry : element.getAsJsonObject().entrySet()) {
            if (useCounts.size() >= MAX_TRACKED_HOMES && !useCounts.containsKey(HomeNames.key(entry.getKey()))) break;
            HomeNames.validate(entry.getKey()).ifPresent(name -> {
                int count = boundedInt(entry.getValue(), 0, 0, Integer.MAX_VALUE);
                if (count > 0) {
                    if (merge) useCounts.merge(HomeNames.key(name), count, ModConfig::saturatedAdd);
                    else useCounts.put(HomeNames.key(name), count);
                }
            });
        }
    }

    private void readCoords(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        for (var entry : element.getAsJsonObject().entrySet()) {
            if (homeCoords.size() >= MAX_TRACKED_HOMES) break;
            HomeNames.validate(entry.getKey()).ifPresent(name -> readCoordinate(HomeNames.key(name), entry.getValue()));
        }
    }

    private void readCoordinate(String key, JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        JsonObject value = element.getAsJsonObject();
        int x = boundedInt(value.get("x"), 0, -30_000_000, 30_000_000);
        int y = boundedInt(value.get("y"), 0, -2048, 2048);
        int z = boundedInt(value.get("z"), 0, -30_000_000, 30_000_000);
        homeCoords.put(key, new HomeCoords(x, y, z));
    }

    private void readHistory(JsonElement element, boolean append) {
        if (element == null || !element.isJsonArray()) return;
        if (!append) history.clear();
        for (JsonElement item : element.getAsJsonArray()) {
            if (history.size() >= MAX_HISTORY) break;
            String name;
            long timestamp;
            if (item.isJsonPrimitive()) {
                name = stringValue(item);
                timestamp = System.currentTimeMillis();
            } else if (item.isJsonObject()) {
                JsonObject object = item.getAsJsonObject();
                name = stringValue(object.has("homeName") ? object.get("homeName") : object.get("name"));
                timestamp = longValue(object.get("timestamp"), System.currentTimeMillis());
            } else continue;
            HomeNames.validate(name).ifPresent(valid -> {
                history.removeIf(entry -> entry.homeName.equalsIgnoreCase(valid));
                history.add(new HistoryEntry(valid, saneTimestamp(timestamp)));
            });
        }
        history.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        trimHistory();
    }

    private JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("themeIndex", themeIndex);
        root.addProperty("compactMode", compactMode);
        root.addProperty("transparentMenu", transparentMenu);
        root.addProperty("language", language);
        root.addProperty("sortMode", sortMode);
        root.addProperty("viewMode", viewMode);
        root.addProperty("totalTeleports", totalTeleports);

        JsonArray favoriteArray = new JsonArray();
        favorites.stream().sorted().forEach(favoriteArray::add);
        root.add("favorites", favoriteArray);

        JsonObject counts = new JsonObject();
        useCounts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> counts.addProperty(entry.getKey(), entry.getValue()));
        root.add("useCounts", counts);

        JsonObject coordinates = new JsonObject();
        homeCoords.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject coordinate = new JsonObject();
            coordinate.addProperty("x", entry.getValue().x);
            coordinate.addProperty("y", entry.getValue().y);
            coordinate.addProperty("z", entry.getValue().z);
            coordinates.add(entry.getKey(), coordinate);
        });
        root.add("homeCoords", coordinates);

        JsonArray historyArray = new JsonArray();
        for (HistoryEntry entry : history) {
            JsonObject item = new JsonObject();
            item.addProperty("homeName", entry.homeName);
            item.addProperty("timestamp", entry.timestamp);
            historyArray.add(item);
        }
        root.add("history", historyArray);
        return root;
    }

    private void save() {
        if (!writeAtomically(configPath(), toJson())) {
            HomeGuiClient.LOGGER.error("Could not save HomeGUI configuration");
        }
    }

    private static boolean writeAtomically(Path destination, JsonObject value) {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            Files.writeString(temporary, GSON.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            return false;
        }
    }

    private static void preserveCorruptFile(Path source) {
        try {
            String suffix = ".corrupt-" + Instant.now().toEpochMilli();
            Files.move(source, source.resolveSibling(source.getFileName() + suffix), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    private void resetDefaults() {
        themeIndex = 0;
        compactMode = false;
        transparentMenu = false;
        language = "en";
        sortMode = "DEFAULT";
        viewMode = "list";
        totalTeleports = 0;
        favorites.clear();
        useCounts.clear();
        homeCoords.clear();
        history.clear();
    }

    private void trimHistory() {
        if (history.size() > MAX_HISTORY) history.subList(MAX_HISTORY, history.size()).clear();
    }

    private static int saturatedIncrement(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static long saneTimestamp(long value) {
        long now = System.currentTimeMillis();
        return value < 0 || value > now + 86_400_000L ? now : value;
    }

    private static String stringValue(JsonElement value) {
        try { return value != null && value.isJsonPrimitive() ? value.getAsString().strip() : ""; }
        catch (RuntimeException ignored) { return ""; }
    }

    private static boolean booleanValue(JsonElement value, boolean fallback) {
        try { return value != null ? value.getAsBoolean() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int boundedInt(JsonElement value, int fallback, int minimum, int maximum) {
        try { return Math.max(minimum, Math.min(maximum, value.getAsInt())); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static long longValue(JsonElement value, long fallback) {
        try { return value != null ? value.getAsLong() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    public static final class HistoryEntry {
        public final String homeName;
        public final long timestamp;

        public HistoryEntry(String name, long timestamp) {
            this.homeName = name;
            this.timestamp = timestamp;
        }

        public String getTimeAgo() {
            long seconds = Math.max(0, (System.currentTimeMillis() - timestamp) / 1000);
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return seconds / 60 + "m";
            if (seconds < 86400) return seconds / 3600 + "h";
            return seconds / 86400 + "d";
        }
    }

    public static final class HomeCoords {
        public final int x;
        public final int y;
        public final int z;

        public HomeCoords(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
