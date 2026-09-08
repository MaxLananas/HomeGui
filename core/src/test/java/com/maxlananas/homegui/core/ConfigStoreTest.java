package com.maxlananas.homegui.core;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {

    @TempDir
    Path dir;

    private void write(String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
    }

    private static String json(String body) {
        return "{" + body + "}";
    }

    // ------------------------------------------------------------- lifecycle

    @Test
    void aFreshInstallReadsNothingAndWritesNothing() {
        ConfigStore store = ConfigStore.open(dir);
        assertEquals(ConfigStore.LoadOutcome.CREATED, store.outcome());
        assertFalse(Files.exists(store.configPath()));
        assertEquals("en", store.preferences().language());
    }

    @Test
    void savingProducesAReadableDocumentAndNoTemporaryFile() throws IOException {
        ConfigStore store = ConfigStore.open(dir);
        store.preferences().setLanguage("fr");
        assertTrue(store.savePreferences());
        assertTrue(Files.isRegularFile(store.configPath()));
        assertFalse(Files.exists(dir.resolve("homegui.json.tmp")));
        assertTrue(JsonParser.parseString(Files.readString(store.configPath())).isJsonObject());
    }

    @Test
    void preferencesSurviveAReload() {
        ConfigStore first = ConfigStore.open(dir);
        first.preferences().setLanguage("de");
        first.preferences().setDensity(Preferences.Density.COMPACT);
        first.preferences().setViewMode(Preferences.ViewMode.GRID);
        first.preferences().setSortMode(SortMode.MOST_USED);
        first.preferences().setGridColumns(5);
        first.preferences().setTransparentMenu(true);
        first.preferences().setAnimationsEnabled(false);
        first.savePreferences();

        ConfigStore second = ConfigStore.open(dir);
        assertEquals(ConfigStore.LoadOutcome.LOADED, second.outcome());
        assertEquals("de", second.preferences().language());
        assertEquals(Preferences.Density.COMPACT, second.preferences().density());
        assertEquals(Preferences.ViewMode.GRID, second.preferences().viewMode());
        assertEquals(SortMode.MOST_USED, second.preferences().sortMode());
        assertEquals(5, second.preferences().gridColumns());
        assertTrue(second.preferences().transparentMenu());
        assertFalse(second.preferences().animationsEnabled());
    }

    // ----------------------------------------------------------- corruption

    @Test
    void anUnreadableFileIsPreservedAndReplacedByDefaults() throws IOException {
        write("homegui.json", "{ this is not json at all");
        ConfigStore store = ConfigStore.open(dir);
        assertEquals(ConfigStore.LoadOutcome.RECOVERED, store.outcome());
        assertEquals("en", store.preferences().language());
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.anyMatch(path ->
                            path.getFileName().toString().startsWith("homegui.json.corrupt-")),
                    "the damaged file must be kept for the player");
        }
        assertTrue(Files.isRegularFile(store.configPath()), "safe defaults are written back");
    }

    @Test
    void aRootThatIsNotAnObjectIsTreatedAsCorrupt() throws IOException {
        write("homegui.json", "[1,2,3]");
        assertEquals(ConfigStore.LoadOutcome.RECOVERED, ConfigStore.open(dir).outcome());
    }

    @Test
    void oneBadFieldDoesNotCostThePlayerTheirData() throws IOException {
        write("homegui.json", json("\"schemaVersion\":3,"
                + "\"preferences\":{\"themeIndex\":\"oops\",\"gridColumns\":99,\"sortMode\":\"NOPE\","
                + "\"language\":\"fr\",\"density\":\"COMPACT\"},"
                + "\"servers\":{\"play.example.net\":{\"favorites\":[\"base\"],\"totalTeleports\":4}}"));
        ConfigStore store = ConfigStore.open(dir);
        assertEquals(ConfigStore.LoadOutcome.LOADED, store.outcome());
        assertEquals(0, store.preferences().themeIndex(), "an unusable theme falls back");
        assertEquals(Preferences.MAX_GRID_COLUMNS, store.preferences().gridColumns(), "an absurd width is clamped");
        assertEquals(SortMode.DEFAULT, store.preferences().sortMode());
        assertEquals("fr", store.preferences().language(), "the good fields survive");
        store.setCurrentServer("play.example.net");
        assertTrue(store.isFavorite("base"));
        assertEquals(4, store.totalTeleports());
    }

    // ----------------------------------------------------------- migration

    @Test
    void aSchema2DocumentIsMigratedIntoTheLegacyServerBucket() throws IOException {
        write("homegui.json", json("\"schemaVersion\":2,\"themeIndex\":1,\"compactMode\":true,"
                + "\"transparentMenu\":true,\"language\":\"fr\",\"sortMode\":\"MOST_USED\",\"viewMode\":\"grid\","
                + "\"totalTeleports\":7,\"favorites\":[\"base\"],\"useCounts\":{\"base\":3},"
                + "\"homeCoords\":{\"base\":{\"x\":1,\"y\":2,\"z\":3}},"
                + "\"history\":[{\"homeName\":\"base\",\"timestamp\":1700000000000}]"));
        ConfigStore store = ConfigStore.open(dir);
        assertEquals(ConfigStore.LoadOutcome.MIGRATED, store.outcome());
        assertEquals(Preferences.Density.COMPACT, store.preferences().density(), "compactMode maps to density");
        assertEquals(Preferences.ViewMode.GRID, store.preferences().viewMode());
        assertTrue(store.isFavorite("base"), "pre server-scoped favourites stay visible");
        assertEquals(3, store.useCount("base"));
        assertEquals(7, store.totalTeleports());
        assertEquals(1, store.history().size());
        assertNotNull(store.coordinates("base"));
        assertEquals(1, store.coordinates("base").x);
    }

    @Test
    void migratingTwiceIsIdempotent() throws IOException {
        write("homegui.json", json("\"schemaVersion\":2,\"favorites\":[\"base\"],\"useCounts\":{\"base\":3}"));
        ConfigStore first = ConfigStore.open(dir);
        first.savePreferences();
        ConfigStore second = ConfigStore.open(dir);
        assertTrue(second.isFavorite("base"));
        assertEquals(3, second.useCount("base"));
    }

    @Test
    void primitiveHistoryEntriesFromVersion3AreStillRead() throws IOException {
        write("homegui.json", json("\"schemaVersion\":2,\"history\":[\"base\",\"farm\"]"));
        ConfigStore store = ConfigStore.open(dir);
        assertEquals(2, store.history().size());
    }

    // ------------------------------------------------------------ statistics

    @Test
    void recordingATeleportUpdatesCountsHistoryAndCoordinates() {
        ConfigStore store = ConfigStore.open(dir);
        store.setCurrentServer("play.example.net");
        store.recordTeleport("base", new Home.Coordinates(1, 2, 3), System.currentTimeMillis());
        store.recordTeleport("base", null, System.currentTimeMillis());
        store.recordTeleport("farm", null, System.currentTimeMillis());

        assertEquals(2, store.useCount("base"));
        assertEquals(1, store.useCount("farm"));
        assertEquals(3, store.totalTeleports());
        assertEquals(2, store.history().size(), "the same home is not listed twice");
        assertEquals(1, store.coordinates("base").x);
    }

    @Test
    void anUnsafeNameIsNeverRecorded() {
        ConfigStore store = ConfigStore.open(dir);
        store.recordTeleport("base\n/op me", null, System.currentTimeMillis());
        assertEquals(0, store.totalTeleports());
        assertTrue(store.history().isEmpty());
    }

    @Test
    void historyIsBounded() {
        ConfigStore store = ConfigStore.open(dir);
        for (int i = 0; i < ConfigStore.MAX_HISTORY + 20; i++) {
            store.recordTeleport("home" + i, null, System.currentTimeMillis() - i);
        }
        assertEquals(ConfigStore.MAX_HISTORY, store.history().size());
    }

    @Test
    void historyIsNewestFirst() {
        ConfigStore store = ConfigStore.open(dir);
        long now = System.currentTimeMillis();
        store.recordTeleport("old", null, now - 10_000);
        store.recordTeleport("new", null, now);
        assertEquals("new", store.history().get(0).homeName);
    }

    @Test
    void serverReportedCoordinatesReplaceAnEstimate() {
        ConfigStore store = ConfigStore.open(dir);
        store.setCurrentServer("play.example.net");
        store.captureCoordinates("base", new Home.Coordinates(0, 0, 0));
        store.rememberReportedCoordinates(java.util.List.of(
                new Home("base", "world", new Home.Coordinates(120, 65, -40))));
        assertEquals(120, store.coordinates("base").x);
    }

    // ---------------------------------------------------- server isolation

    @Test
    void dataIsKeptApartBetweenServers() {
        ConfigStore store = ConfigStore.open(dir);
        store.setCurrentServer("a.example.net");
        assertTrue(store.toggleFavorite("base"));
        store.recordTeleport("mine", null, System.currentTimeMillis());

        store.setCurrentServer("b.example.net");
        assertFalse(store.isFavorite("base"));
        assertEquals(0, store.useCount("mine"));
        assertTrue(store.history().isEmpty());

        store.setCurrentServer("a.example.net");
        assertTrue(store.isFavorite("base"));
        assertEquals(1, store.useCount("mine"));
    }

    @Test
    void serversSurviveAReload() {
        ConfigStore first = ConfigStore.open(dir);
        first.setCurrentServer("a.example.net");
        first.toggleFavorite("base");
        first.savePreferences();

        ConfigStore second = ConfigStore.open(dir);
        second.setCurrentServer("a.example.net");
        assertTrue(second.isFavorite("base"));
        second.setCurrentServer("b.example.net");
        assertFalse(second.isFavorite("base"));
    }

    @Test
    void anUntrustedServerKeyFromDiskIsRejected() throws IOException {
        write("homegui.json", json("\"schemaVersion\":3,"
                + "\"servers\":{\"../escape\":{\"favorites\":[\"base\"]},\"good.example.net\":{\"favorites\":[\"farm\"]}}"));
        ConfigStore store = ConfigStore.open(dir);
        store.setCurrentServer("good.example.net");
        assertTrue(store.isFavorite("farm"));
        assertFalse(store.isFavorite("base"));
    }

    // --------------------------------------------------- export and import

    @Test
    void exportThenImportReproducesTheData() {
        ConfigStore source = ConfigStore.open(dir);
        source.setCurrentServer("play.example.net");
        source.toggleFavorite("base");
        source.recordTeleport("mine", new Home.Coordinates(7, 8, 9), System.currentTimeMillis());
        assertTrue(source.exportData());

        Path otherDir = dir.resolve("other");
        ConfigStore target = ConfigStore.open(otherDir);
        target.setCurrentServer("play.example.net");
        ConfigStore.ImportResult result = target.importFrom(source.exportPath());
        assertTrue(result.isSuccess());
        assertTrue(target.isFavorite("base"));
        assertEquals(1, target.useCount("mine"));
    }

    @Test
    void aVersion1ExportIsMigratedDuringImport() throws IOException {
        write("homegui-export.json", json("\"format\":\"homegui-v1\",\"homes\":{"
                + "\"base\":{\"favorite\":true,\"use_count\":5,\"coords\":{\"x\":1,\"y\":2,\"z\":3}},"
                + "\"bad name\":{\"favorite\":true}}"));
        ConfigStore store = ConfigStore.open(dir);
        ConfigStore.ImportResult result = store.importData();
        assertTrue(result.isSuccess());
        assertTrue(result.imported >= 1);
        assertTrue(store.isFavorite("base"));
        assertEquals(5, store.useCount("base"));
        assertFalse(store.isFavorite("bad name"));
    }

    @Test
    void importingWithoutAFileIsReportedClearly() {
        ConfigStore store = ConfigStore.open(dir);
        assertTrue(store.importData().fileMissing);
    }

    @Test
    void importingGarbageFailsWithoutTouchingTheStore() throws IOException {
        write("homegui-export.json", "not json");
        ConfigStore store = ConfigStore.open(dir);
        store.toggleFavorite("base");
        ConfigStore.ImportResult result = store.importData();
        assertFalse(result.isSuccess());
        assertNotNull(result.error);
        assertTrue(store.isFavorite("base"));
    }

    @Test
    void anImportIsBounded() throws IOException {
        StringBuilder favorites = new StringBuilder();
        for (int i = 0; i < ConfigStore.MAX_TRACKED_HOMES + 1_000; i++) {
            if (i > 0) favorites.append(',');
            favorites.append("\"home").append(i).append('"');
        }
        write("homegui-export.json", json("\"favorites\":[" + favorites + "]"));
        ConfigStore store = ConfigStore.open(dir);
        store.importData();
        assertEquals(ConfigStore.MAX_TRACKED_HOMES, store.favoriteKeys().size());
    }
}
