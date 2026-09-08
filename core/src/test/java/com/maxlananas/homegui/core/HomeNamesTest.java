package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeNamesTest {

    @Test
    void acceptsNamesHomePluginsActuallyUse() {
        for (String name : new String[]{"base", "Farm", "my_home", "nether.hub", "shop:2", "a+b", "Café",
                "дом", "家", "홈", "ホーム"}) {
            assertTrue(HomeNames.isValid(name), "expected to accept " + name);
        }
    }

    @Test
    void rejectsAnythingThatCouldCarryASecondCommand() {
        for (String name : new String[]{"", "   ", "two words", "base\n/op me", "base;op", "a/b",
                "../etc", "base\ttab", "\u0000nul", "§cbad", "base|pipe"}) {
            assertFalse(HomeNames.isValid(name), "expected to reject " + name);
        }
    }

    @Test
    void rejectsRunsOfDecorationThatAreNotNames() {
        assertFalse(HomeNames.isValid("---"));
        assertFalse(HomeNames.isValid("..."));
        assertFalse(HomeNames.isValid("___"));
    }

    @Test
    void enforcesTheLengthLimit() {
        String tooLong = "a".repeat(HomeNames.MAX_LENGTH + 1);
        assertFalse(HomeNames.isValid(tooLong));
        assertTrue(HomeNames.isValid("a".repeat(HomeNames.MAX_LENGTH)));
    }

    @Test
    void trimsSurroundingWhitespaceWithoutChangingTheName() {
        assertEquals("base", HomeNames.validate("  base  ").orElseThrow());
    }

    @Test
    void keysAreLocaleIndependent() {
        assertEquals("caf\u00e9", HomeNames.key("Caf\u00E9"));
        assertEquals("istanbul", HomeNames.key("ISTANBUL"));
    }
}
