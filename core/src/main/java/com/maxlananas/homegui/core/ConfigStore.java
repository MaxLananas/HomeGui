package com.maxlananas.homegui.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Local persistence for HomeGui.
 *
 * <p>Everything HomeGui remembers lives in one JSON document: interface preferences,
 * and per-server favourites, use counts, captured coordinates and history. Nothing
 * server side is ever written, and no credential or token is ever stored.
 *
 * <p>Robustness rules that are enforced here and covered by tests:
 * <ul>
 *   <li>writes go to a temporary file and are then atomically moved into place;</li>
 *   <li>a file that cannot be parsed at all is preserved as
 *       {@code homegui.json.corrupt-<timestamp>} instead of being deleted;</li>
 *   <li>individual invalid fields are skipped, so one bad number cannot cost the
 *       player their favourites;</li>
 *   <li>collections are bounded, so a hostile or damaged file cannot grow without
 *       limit;</li>
 *   <li>schema 1 and 2 documents are migrated on read, and the migration is
 *       idempotent.</li>
 * </ul>
 *
 * <p>Data written before HomeGui knew which server it was talking to is kept under
 * the {@value #LEGACY_SERVER} bucket and stays visible everywhere as a fallback; a
 * per-server entry always wins over it.
 */
public final class ConfigStore {

    public static final int SCHEMA_VERSION = 3;
    public static final String LEGACY_SERVER = "*";

    public static final int MAX_TRACKED_HOMES = 2_000;
    public static final int MAX_HISTORY = 25;
    public static final int MAX_SERVERS = 64;
    public static final long MAX_FILE_BYTES = 2_000_000L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** What happened while the file was read. Surfaced in the interface and in logs. */
    public enum LoadOutcome {
        /** No file yet; defaults were used and nothing was written. */
        CREATED,
        /** A current schema document was read. */
        LOADED,
        /** An older schema document was read and migrated in memory. */
        MIGRATED,
        /** The file could not be read; it was backed up and defaults were used. */
        RECOVERED
    }

    /** Result of an import, precise enough to tell the player what happened. */
    public static final class ImportResult {
        public final int imported;
        public final int skipped;
        public final boolean fileMissing;
        public final String error;

        ImportResult(int imported, int skipped, boolean fileMissing, String error) {
            this.imported = imported;
            this.skipped = skipped;
            this.fileMissing = fileMissing;
            this.error = error;
        }

        static ImportResult success(int imported, int skipped) {
            return new ImportResult(imported, skipped, false, null);
        }

        static ImportResult missing() {
            return new ImportResult(0, 0, true, null);
        }

        static ImportResult failure(String error) {
            return new ImportResult(0, 0, false, error);
        }

        public boolean isSuccess() { return error == null && !fileMissing; }
    }

    /** One remembered teleport. */
    public static final class HistoryEntry {
        public final String homeName;
        public final long timestamp;

        public HistoryEntry(String homeName, long timestamp) {
            this.homeName = homeName;
            this.timestamp = timestamp;
        }

        public String key() { return HomeNames.key(homeName); }
    }

    /** Everything HomeGui remembers about one server. */
    private static final class ServerData {
        final Set<String> favorites = new LinkedHashSet<>();
        final Map<String, Integer> useCounts = new LinkedHashMap<>();
        final Map<String, Home.Coordinates> coordinates = new LinkedHashMap<>();
        final List<HistoryEntry> history = new ArrayList<>();
        int totalTeleports;
        long lastSeen;

        ServerData copy() {
            ServerData copy = new ServerData();
            copy.favorites.addAll(favorites);
            copy.useCounts.putAll(useCounts);
            copy.coordinates.putAll(coordinates);
            for (HistoryEntry entry : history) copy.history.add(entry);
            copy.totalTeleports = totalTeleports;
            copy.lastSeen = lastSeen;
            return copy;
        }
    }

    private final Path directory;
    private final Preferences preferences = new Preferences();
    private final Map<String, ServerData> servers = new LinkedHashMap<>();

    private String currentServer = ServerKey.LOCAL;
    private LoadOutcome outcome = LoadOutcome.CREATED;
    private long lastWriteFailure;
    private boolean persistent = true;

    private ConfigStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    /** Reads {@code homegui.json} from {@code directory}, migrating or recovering as needed. */
    public static ConfigStore open(Path directory) {
        ConfigStore store = new ConfigStore(directory);
        store.load();
        return store;
    }

    /** Creates an in-memory store that never touches disk. Used by tests. */
    public static ConfigStore inMemory() {
        ConfigStore store = new ConfigStore(Path.of("."));
        store.outcome = LoadOutcome.CREATED;
        store.persistent = false;
        return store;
    }

    /** True when changes are written to disk. */
    public boolean isPersistent() { return persistent; }

    public Path directory() { return directory; }

    public Path configPath() { return directory.resolve("homegui.json"); }

    public Path exportPath() { return directory.resolve("homegui-export.json"); }

    public LoadOutcome outcome() { return outcome; }

    public long lastWriteFailure() { return lastWriteFailure; }

    // ---------------------------------------------------------------- servers

    public String currentServer() { return currentServer; }

    public void setCurrentServer(String serverKey) {
        currentServer = ServerKey.sanitise(serverKey);
        server(currentServer).lastSeen = System.currentTimeMillis();
    }

    private ServerData server(String key) {
        ServerData data = servers.get(key);
        if (data == null) {
            data = new ServerData();
            servers.put(key, data);
        }
        return data;
    }

    private ServerData current() { return server(currentServer); }

    private ServerData legacy() { return servers.get(LEGACY_SERVER); }

    // ---------------------------------------------------------- preferences

    public Preferences preferences() { return preferences; }

    /** Persists preferences; the caller can tell the user if the write failed. */
    public boolean savePreferences() { return save(); }

    // ----------------------------------------------------------- favourites

    public boolean isFavorite(String home) {
        String key = HomeNames.key(home);
        if (current().favorites.contains(key)) return true;
        ServerData fallback = legacy();
        return fallback != null && fallback.favorites.contains(key);
    }

    /** @return the new state of the favourite flag. */
    public boolean toggleFavorite(String home) {
        Optional<String> name = HomeNames.validate(home);
        if (name.isEmpty()) return false;
        String key = HomeNames.key(name.get());
        ServerData data = current();
        boolean nowFavorite;
        if (data.favorites.contains(key)) {
            data.favorites.remove(key);
            nowFavorite = false;
        } else {
            if (data.favorites.size() >= MAX_TRACKED_HOMES) return isFavorite(home);
            data.favorites.add(key);
            nowFavorite = true;
        }
        save();
        return nowFavorite;
    }

    public Set<String> favoriteKeys() {
        Set<String> keys = new LinkedHashSet<>(current().favorites);
        ServerData fallback = legacy();
        if (fallback != null) keys.addAll(fallback.favorites);
        return Collections.unmodifiableSet(keys);
    }

    // ---------------------------------------------------------- statistics

    public int useCount(String home) {
        String key = HomeNames.key(home);
        Integer count = current().useCounts.get(key);
        if (count != null) return count;
        ServerData fallback = legacy();
        return fallback == null ? 0 : fallback.useCounts.getOrDefault(key, 0);
    }

    /** Use counts for the current server, with legacy entries filling the gaps. */
    public Map<String, Integer> useCounts() {
        Map<String, Integer> merged = new TreeMap<>();
        ServerData fallback = legacy();
        if (fallback != null) merged.putAll(fallback.useCounts);
        merged.putAll(current().useCounts);
        return Collections.unmodifiableMap(merged);
    }

    public int totalTeleports() {
        ServerData fallback = legacy();
        return current().totalTeleports + (fallback == null ? 0 : fallback.totalTeleports);
    }

    /**
     * Records a teleport that was actually confirmed. Called from
     * {@link HomesController} once the player has moved, never when the command was
     * merely sent, so refused teleports do not inflate the counters.
     */
    public void recordTeleport(String home, Home.Coordinates destination, long timestamp) {
        Optional<String> valid = HomeNames.validate(home);
        if (valid.isEmpty()) return;
        String display = valid.get();
        String key = HomeNames.key(display);
        ServerData data = current();
        long now = System.currentTimeMillis();

        if (data.useCounts.size() < MAX_TRACKED_HOMES || data.useCounts.containsKey(key)) {
            Integer previous = data.useCounts.get(key);
            data.useCounts.put(key, previous == null ? 1 : saturatedIncrement(previous));
        }
        data.totalTeleports = saturatedIncrement(data.totalTeleports);
        data.lastSeen = now;

        data.history.removeIf(entry -> entry.homeName.equalsIgnoreCase(display));
        data.history.add(0, new HistoryEntry(display, saneTimestamp(timestamp)));
        trim(data.history);

        if (destination != null
                && (data.coordinates.size() < MAX_TRACKED_HOMES || data.coordinates.containsKey(key))) {
            data.coordinates.put(key, destination);
        }
        save();
    }

    public List<HistoryEntry> history() {
        List<HistoryEntry> entries = new ArrayList<>(current().history);
        entries.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return Collections.unmodifiableList(entries);
    }

    public void clearHistory() {
        current().history.clear();
        save();
    }

    /** Implements {@link SortMode.HomeIndex} against the current server. */
    public SortMode.HomeIndex index() {
        return new SortMode.HomeIndex() {
            @Override
            public boolean isFavorite(String home) { return ConfigStore.this.isFavorite(home); }

            @Override
            public int useCount(String home) { return ConfigStore.this.useCount(home); }

            @Override
            public int recencyRank(String home) {
                String key = HomeNames.key(home);
                List<HistoryEntry> entries = history();
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).key().equals(key)) return i;
                }
                return Integer.MAX_VALUE;
            }
        };
    }

    // -------------------------------------------------------- coordinates

    public Home.Coordinates coordinates(String home) {
        String key = HomeNames.key(home);
        Home.Coordinates value = current().coordinates.get(key);
        if (value != null) return value;
        ServerData fallback = legacy();
        return fallback == null ? null : fallback.coordinates.get(key);
    }

    /**
     * Stores an approximate destination captured on the client. Server-reported
     * coordinates always win, so this never overwrites an exact position.
     */
    public void captureCoordinates(String home, Home.Coordinates position) {
        Optional<String> valid = HomeNames.validate(home);
        if (valid.isEmpty() || position == null) return;
        String key = HomeNames.key(valid.get());
        ServerData data = current();
        if (data.coordinates.size() >= MAX_TRACKED_HOMES && !data.coordinates.containsKey(key)) return;
        data.coordinates.put(key, position);
        save();
    }

    /** Stores coordinates the server itself reported, replacing any estimate. */
    public void rememberReportedCoordinates(List<Home> homes) {
        ServerData data = current();
        boolean changed = false;
        for (Home home : homes) {
            if (!home.hasCoordinates()) continue;
            String key = home.key();
            if (data.coordinates.size() >= MAX_TRACKED_HOMES && !data.coordinates.containsKey(key)) continue;
            data.coordinates.put(key, home.coordinates().orElse(null));
            changed = true;
        }
        if (changed) save();
    }

    // ----------------------------------------------------- import / export

    /** Writes {@code homegui-export.json} next to the configuration file. */
    public boolean exportData() {
        if (!persistent) return false;
        JsonObject root = toJson();
        root.addProperty("format", "homegui-v3");
        root.addProperty("exportedAt", System.currentTimeMillis());
        return writeAtomically(exportPath(), root);
    }

    /** Merges {@code homegui-export.json} into the current store. */
    public ImportResult importData() {
        return importFrom(exportPath());
    }

    public ImportResult importFrom(Path path) {
        if (path == null || !Files.isRegularFile(path)) return ImportResult.missing();
        try {
            if (Files.size(path) > MAX_FILE_BYTES) {
                return ImportResult.failure("file exceeds " + (MAX_FILE_BYTES / 1024) + " kB");
            }
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return ImportResult.failure("root must be an object");
            int[] counts = merge(parsed.getAsJsonObject());
            save();
            return ImportResult.success(counts[0], counts[1]);
        } catch (RuntimeException | IOException exception) {
            return ImportResult.failure(exception.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------ reading

    private void load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            outcome = LoadOutcome.CREATED;
            return;
        }
        try {
            if (Files.size(path) > MAX_FILE_BYTES) throw new IOException("file exceeds the size limit");
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("root must be an object");
            JsonObject root = parsed.getAsJsonObject();
            int schema = intValue(root.get("schemaVersion"), 1);
            readPreferences(root.has("preferences") && root.get("preferences").isJsonObject()
                    ? root.getAsJsonObject("preferences") : root);
            if (root.has("servers") && root.get("servers").isJsonObject()) {
                readServers(root.getAsJsonObject("servers"));
            } else {
                migrateLegacyDocument(root);
            }
            outcome = schema < SCHEMA_VERSION ? LoadOutcome.MIGRATED : LoadOutcome.LOADED;
        } catch (RuntimeException | IOException exception) {
            preserveCorruptFile(path);
            resetInMemory();
            save();
            outcome = LoadOutcome.RECOVERED;
        }
    }

    /** Schema 1 and 2 stored everything at the top level and knew no servers. */
    private void migrateLegacyDocument(JsonObject root) {
        ServerData data = server(LEGACY_SERVER);
        readFavorites(root.get("favorites"), data);
        readCounts(root.get("useCounts"), data, false);
        readCoordinates(root.get("homeCoords"), data);
        readCoordinates(root.get("coords"), data);
        readHistory(root.get("history"), data);
        data.totalTeleports = boundedInt(root.get("totalTeleports"), 0, 0, Integer.MAX_VALUE);
        if (root.has("homes") && root.get("homes").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("homes").entrySet()) {
                Optional<String> valid = HomeNames.validate(entry.getKey());
                if (valid.isEmpty() || !entry.getValue().isJsonObject()) continue;
                String key = HomeNames.key(valid.get());
                JsonObject value = entry.getValue().getAsJsonObject();
                if (data.favorites.size() < MAX_TRACKED_HOMES && booleanValue(value.get("favorite"), false)) {
                    data.favorites.add(key);
                }
                int uses = boundedInt(value.get("use_count"), 0, 0, Integer.MAX_VALUE);
                if (uses > 0 && data.useCounts.size() < MAX_TRACKED_HOMES) {
                    data.useCounts.merge(key, uses, ConfigStore::saturatedAdd);
                }
                readCoordinate(key, value.get("coords"), data);
            }
        }
    }

    private void readServers(JsonObject root) {
        int read = 0;
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (read >= MAX_SERVERS) break;
            String key = entry.getKey();
            if (!ServerKey.isValid(key) && !LEGACY_SERVER.equals(key)) continue;
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject value = entry.getValue().getAsJsonObject();
            ServerData data = server(key);
            readFavorites(value.get("favorites"), data);
            readCounts(value.get("useCounts"), data, false);
            readCoordinates(value.get("coords"), data);
            readCoordinates(value.get("homeCoords"), data);
            readHistory(value.get("history"), data);
            data.totalTeleports = boundedInt(value.get("totalTeleports"), 0, 0, Integer.MAX_VALUE);
            data.lastSeen = longValue(value.get("lastSeen"), 0L);
            read++;
        }
    }

    /** @return {@code {imported, skipped}}. */
    private int[] merge(JsonObject root) {
        int imported = 0;
        int skipped = 0;

        if (root.has("preferences") && root.get("preferences").isJsonObject()) {
            readPreferences(root.getAsJsonObject("preferences"));
        } else if (!root.has("servers")) {
            readPreferences(root);
        }

        ServerData data = current();
        if (root.has("servers") && root.get("servers").isJsonObject()) {
            JsonObject all = root.getAsJsonObject("servers");
            JsonElement mine = all.get(currentServer);
            JsonElement legacyEntry = all.get(LEGACY_SERVER);
            if (mine != null && mine.isJsonObject()) {
                int[] counts = mergeServer(mine.getAsJsonObject(), data);
                imported += counts[0];
                skipped += counts[1];
            } else if (legacyEntry != null && legacyEntry.isJsonObject()) {
                int[] counts = mergeServer(legacyEntry.getAsJsonObject(), data);
                imported += counts[0];
                skipped += counts[1];
            }
        } else {
            int[] counts = mergeServer(root, data);
            imported += counts[0];
            skipped += counts[1];
            // Schema 1 exports described every tracked home as an object under "homes".
            if (root.has("homes") && root.get("homes").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("homes").entrySet()) {
                    Optional<String> valid = HomeNames.validate(entry.getKey());
                    if (valid.isEmpty() || !entry.getValue().isJsonObject()) {
                        skipped++;
                        continue;
                    }
                    String key = HomeNames.key(valid.get());
                    JsonObject value = entry.getValue().getAsJsonObject();
                    if (booleanValue(value.get("favorite"), false) && data.favorites.size() < MAX_TRACKED_HOMES) {
                        data.favorites.add(key);
                    }
                    int uses = boundedInt(value.get("use_count"), 0, 0, Integer.MAX_VALUE);
                    if (uses > 0 && data.useCounts.size() < MAX_TRACKED_HOMES) {
                        data.useCounts.merge(key, uses, ConfigStore::saturatedAdd);
                    }
                    readCoordinate(key, value.get("coords"), data);
                    imported++;
                }
            }
        }
        return new int[]{imported, skipped};
    }

    private int[] mergeServer(JsonObject value, ServerData data) {
        int before = data.favorites.size() + data.useCounts.size() + data.history.size();
        readFavorites(value.get("favorites"), data);
        readCounts(value.get("useCounts"), data, true);
        readCoordinates(value.get("coords"), data);
        readCoordinates(value.get("homeCoords"), data);
        readHistory(value.get("history"), data);
        int after = data.favorites.size() + data.useCounts.size() + data.history.size();
        return new int[]{Math.max(0, after - before), 0};
    }

    private void readPreferences(JsonObject root) {
        preferences.setThemeIndex(boundedInt(root.get("themeIndex"), 0, 0, Preferences.THEME_COUNT - 1));
        preferences.setLanguage(stringValue(root.get("language")));
        preferences.setSortMode(SortMode.fromString(stringValue(root.get("sortMode"))));
        preferences.setViewMode(Preferences.ViewMode.fromString(stringValue(root.get("viewMode"))));
        preferences.setGridColumns(boundedInt(root.get("gridColumns"), 3,
                Preferences.MIN_GRID_COLUMNS, Preferences.MAX_GRID_COLUMNS));
        preferences.setTransparentMenu(booleanValue(root.get("transparentMenu"), false));
        preferences.setShowCoordinates(booleanValue(root.get("showCoordinates"), true));
        preferences.setShowUseCounts(booleanValue(root.get("showUseCounts"), true));
        preferences.setAnimationsEnabled(booleanValue(root.get("animationsEnabled"), true));
        preferences.setConfirmDestructive(booleanValue(root.get("confirmDestructive"), true));
        // Schema 2 called this compactMode and had no density name.
        if (root.has("density")) {
            preferences.setDensity(Preferences.Density.fromString(stringValue(root.get("density"))));
        } else {
            preferences.setDensity(booleanValue(root.get("compactMode"), false)
                    ? Preferences.Density.COMPACT : Preferences.Density.COMFORTABLE);
        }
    }

    private void readFavorites(JsonElement element, ServerData data) {
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement item : element.getAsJsonArray()) {
            if (data.favorites.size() >= MAX_TRACKED_HOMES) return;
            HomeNames.validate(stringValue(item)).ifPresent(name -> data.favorites.add(HomeNames.key(name)));
        }
    }

    private void readCounts(JsonElement element, ServerData data, boolean merge) {
        if (element == null || !element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            HomeNames.validate(entry.getKey()).ifPresent(name -> {
                String key = HomeNames.key(name);
                if (data.useCounts.size() >= MAX_TRACKED_HOMES && !data.useCounts.containsKey(key)) return;
                int count = boundedInt(entry.getValue(), 0, 0, Integer.MAX_VALUE);
                if (count <= 0) return;
                if (merge) data.useCounts.merge(key, count, ConfigStore::saturatedAdd);
                else data.useCounts.put(key, count);
            });
        }
    }

    private void readCoordinates(JsonElement element, ServerData data) {
        if (element == null || !element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            HomeNames.validate(entry.getKey())
                    .ifPresent(name -> readCoordinate(HomeNames.key(name), entry.getValue(), data));
        }
    }

    private void readCoordinate(String key, JsonElement element, ServerData data) {
        if (element == null || !element.isJsonObject()) return;
        if (data.coordinates.size() >= MAX_TRACKED_HOMES && !data.coordinates.containsKey(key)) return;
        JsonObject value = element.getAsJsonObject();
        int x = boundedInt(value.get("x"), 0, -30_000_000, 30_000_000);
        int y = boundedInt(value.get("y"), 0, -4096, 4096);
        int z = boundedInt(value.get("z"), 0, -30_000_000, 30_000_000);
        data.coordinates.put(key, new Home.Coordinates(x, y, z));
    }

    private void readHistory(JsonElement element, ServerData data) {
        if (element == null || !element.isJsonArray()) return;
        long now = System.currentTimeMillis();
        for (JsonElement item : element.getAsJsonArray()) {
            if (data.history.size() >= MAX_HISTORY) break;
            String name;
            long timestamp;
            if (item.isJsonPrimitive()) {
                name = stringValue(item);
                timestamp = now;
            } else if (item.isJsonObject()) {
                JsonObject object = item.getAsJsonObject();
                name = stringValue(object.has("homeName") ? object.get("homeName") : object.get("name"));
                timestamp = longValue(object.get("timestamp"), now);
            } else {
                continue;
            }
            HomeNames.validate(name).ifPresent(valid -> {
                data.history.removeIf(entry -> entry.homeName.equalsIgnoreCase(valid));
                data.history.add(new HistoryEntry(valid, saneTimestamp(timestamp)));
            });
        }
        data.history.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        trim(data.history);
    }

    // ------------------------------------------------------------ writing

    private JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add("preferences", preferencesToJson());

        JsonObject all = new JsonObject();
        servers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> all.add(entry.getKey(), serverToJson(entry.getValue())));
        root.add("servers", all);
        return root;
    }

    private JsonObject preferencesToJson() {
        JsonObject json = new JsonObject();
        json.addProperty("language", preferences.language());
        json.addProperty("themeIndex", preferences.themeIndex());
        json.addProperty("density", preferences.density().name());
        json.addProperty("viewMode", preferences.viewMode().name());
        json.addProperty("sortMode", preferences.sortMode().name());
        json.addProperty("gridColumns", preferences.gridColumns());
        json.addProperty("transparentMenu", preferences.transparentMenu());
        json.addProperty("showCoordinates", preferences.showCoordinates());
        json.addProperty("showUseCounts", preferences.showUseCounts());
        json.addProperty("animationsEnabled", preferences.animationsEnabled());
        json.addProperty("confirmDestructive", preferences.confirmDestructive());
        return json;
    }

    private JsonObject serverToJson(ServerData data) {
        JsonObject json = new JsonObject();

        JsonArray favorites = new JsonArray();
        data.favorites.stream().sorted().forEach(favorites::add);
        json.add("favorites", favorites);

        JsonObject counts = new JsonObject();
        data.useCounts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> counts.addProperty(entry.getKey(), entry.getValue()));
        json.add("useCounts", counts);

        JsonObject coordinates = new JsonObject();
        data.coordinates.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Home.Coordinates value = entry.getValue();
                    if (value == null) return;
                    JsonObject coordinate = new JsonObject();
                    coordinate.addProperty("x", value.x);
                    coordinate.addProperty("y", value.y);
                    coordinate.addProperty("z", value.z);
                    coordinates.add(entry.getKey(), coordinate);
                });
        json.add("coords", coordinates);

        JsonArray history = new JsonArray();
        for (HistoryEntry entry : data.history) {
            JsonObject item = new JsonObject();
            item.addProperty("homeName", entry.homeName);
            item.addProperty("timestamp", entry.timestamp);
            history.add(item);
        }
        json.add("history", history);

        json.addProperty("totalTeleports", data.totalTeleports);
        json.addProperty("lastSeen", data.lastSeen);
        return json;
    }

    /** Persists the whole document in one atomic replacement. */
    public boolean save() {
        if (!persistent) return true;
        if (!writeAtomically(configPath(), toJson())) {
            lastWriteFailure = System.currentTimeMillis();
            return false;
        }
        return true;
    }

    private boolean writeAtomically(Path destination, JsonObject value) {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            Files.writeString(temporary, GSON.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // nothing else can be done; the next save will try again
            }
            return false;
        }
    }

    private static void preserveCorruptFile(Path source) {
        try {
            String suffix = ".corrupt-" + Instant.now().toEpochMilli();
            Files.move(source, source.resolveSibling(source.getFileName() + suffix),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // the file stays where it is, which is still better than deleting it
        }
    }

    private void resetInMemory() {
        servers.clear();
        preferences.setLanguage("en");
        preferences.setThemeIndex(0);
        preferences.setDensity(Preferences.Density.COMFORTABLE);
        preferences.setViewMode(Preferences.ViewMode.LIST);
        preferences.setSortMode(SortMode.DEFAULT);
        preferences.setGridColumns(3);
        preferences.setTransparentMenu(false);
        preferences.setShowCoordinates(true);
        preferences.setShowUseCounts(true);
        preferences.setAnimationsEnabled(true);
        preferences.setConfirmDestructive(true);
    }

    // ------------------------------------------------------------ helpers

    private static void trim(List<HistoryEntry> history) {
        if (history.size() > MAX_HISTORY) history.subList(MAX_HISTORY, history.size()).clear();
    }

    private static int saturatedIncrement(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static long saneTimestamp(long value) {
        long now = System.currentTimeMillis();
        return value < 0 || value > now + 86_400_000L ? now : value;
    }

    private static String stringValue(JsonElement value) {
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsString().strip() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean booleanValue(JsonElement value, boolean fallback) {
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int intValue(JsonElement value, int fallback) {
        return boundedInt(value, fallback, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static int boundedInt(JsonElement value, int fallback, int minimum, int maximum) {
        try {
            return Math.max(minimum, Math.min(maximum, value.getAsInt()));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long longValue(JsonElement value, long fallback) {
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
