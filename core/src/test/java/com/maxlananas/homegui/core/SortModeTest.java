package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortModeTest {

    private static final class Index implements SortMode.HomeIndex {
        final Set<String> favorites;
        final Map<String, Integer> uses = new HashMap<>();
        final List<String> recent = new ArrayList<>();

        Index(Set<String> favorites) {
            this.favorites = favorites;
        }

        @Override
        public boolean isFavorite(String home) {
            return favorites.contains(HomeNames.key(home));
        }

        @Override
        public int useCount(String home) {
            return uses.getOrDefault(HomeNames.key(home), 0);
        }

        @Override
        public int recencyRank(String home) {
            int index = recent.indexOf(HomeNames.key(home));
            return index < 0 ? Integer.MAX_VALUE : index;
        }
    }

    private static List<Home> homes(String... names) {
        List<Home> list = new ArrayList<>();
        for (String name : names) list.add(Home.of(name));
        return list;
    }

    private static List<String> names(List<Home> list) {
        return list.stream().map(Home::name).collect(Collectors.toList());
    }

    @Test
    void defaultKeepsTheOrderTheServerUsed() {
        List<Home> list = homes("zeta", "alpha", "mid");
        SortMode.DEFAULT.apply(list, new Index(Set.of()));
        assertEquals(List.of("zeta", "alpha", "mid"), names(list));
    }

    @Test
    void alphabeticalIgnoresCase() {
        List<Home> list = homes("zeta", "Alpha", "mid");
        SortMode.ALPHABETICAL.apply(list, new Index(Set.of()));
        assertEquals(List.of("Alpha", "mid", "zeta"), names(list));
    }

    @Test
    void mostUsedBreaksTiesAlphabetically() {
        Index index = new Index(Set.of());
        index.uses.put("beta", 5);
        index.uses.put("alpha", 5);
        index.uses.put("gamma", 9);
        List<Home> list = homes("beta", "alpha", "gamma", "delta");
        SortMode.MOST_USED.apply(list, index);
        assertEquals(List.of("gamma", "alpha", "beta", "delta"), names(list));
    }

    @Test
    void recentPutsNeverUsedHomesLast() {
        Index index = new Index(Set.of());
        index.recent.add("mine");
        index.recent.add("base");
        List<Home> list = homes("unused", "base", "mine");
        SortMode.RECENT.apply(list, index);
        assertEquals(List.of("mine", "base", "unused"), names(list));
    }

    @Test
    void favouritesComeFirstAndStayAlphabetical() {
        Index index = new Index(Set.of("zulu", "alpha"));
        List<Home> list = homes("mike", "zulu", "bravo", "alpha");
        SortMode.FAVORITES_FIRST.apply(list, index);
        assertEquals(List.of("alpha", "zulu", "bravo", "mike"), names(list));
    }

    @Test
    void unknownModesFallBackToTheServerOrder() {
        assertEquals(SortMode.DEFAULT, SortMode.fromString("nonsense"));
        assertEquals(SortMode.DEFAULT, SortMode.fromString(null));
        assertEquals(SortMode.MOST_USED, SortMode.fromString("most_used"));
    }

    @Test
    void cyclingVisitsEveryModeExactlyOnce() {
        SortMode mode = SortMode.DEFAULT;
        for (int i = 0; i < SortMode.values().length; i++) mode = mode.next();
        assertEquals(SortMode.DEFAULT, mode);
    }
}

class ServerKeyTest {

    @Test
    void normalisesAnAddress() {
        assertEquals("play.example.net:25565", ServerKey.fromAddress("Play.Example.Net:25565"));
    }

    @Test
    void stripsCharactersThatDoNotBelongInAKey() {
        assertEquals("example.net", ServerKey.fromAddress("../exa\u0000mple.net"));
    }

    @Test
    void treatsLocalAddressesAsSinglePlayer() {
        assertEquals(ServerKey.LOCAL, ServerKey.fromAddress("localhost"));
        assertEquals(ServerKey.LOCAL, ServerKey.fromAddress("127.0.0.1"));
        assertEquals(ServerKey.LOCAL, ServerKey.fromAddress(null));
        assertEquals(ServerKey.LOCAL, ServerKey.fromAddress("   "));
    }

    @Test
    void boundsTheKeyLength() {
        assertTrue(ServerKey.fromAddress("a".repeat(500)).length() <= 160);
    }

    @Test
    void rejectsKeysThatDidNotComeFromThisClass() {
        assertTrue(ServerKey.isValid("play.example.net"));
        assertTrue(ServerKey.isValid(ServerKey.LOCAL));
        assertFalse(ServerKey.isValid("../escape"));
        assertFalse(ServerKey.isValid("Play.Example.Net"), "case is part of the normal form");
        assertFalse(ServerKey.isValid(""));
        assertFalse(ServerKey.isValid(null));
        assertEquals(ServerKey.LOCAL, ServerKey.sanitise("../escape"),
                "a stored key that is not normal is not silently repaired into another bucket");
    }

    @Test
    void repairingAnAddressAndRejectingAKeyAreDifferentOperations() {
        assertEquals("escape", ServerKey.normalise("../escape"));
        assertEquals(ServerKey.LOCAL, ServerKey.sanitise("../escape"));
        assertEquals("play.example.net:25565", ServerKey.normalise("Play.Example.Net:25565"));
        assertEquals(ServerKey.LOCAL, ServerKey.normalise("..."));
        assertEquals(ServerKey.LOCAL, ServerKey.normalise(null));
    }
}
