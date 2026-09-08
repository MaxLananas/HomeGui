package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeCommandTest {

    @Test
    void recognisesACompleteTeleportCommand() {
        assertEquals("Base-1", HomeCommand.teleportTarget("/home Base-1").orElseThrow());
        assertEquals("Base-1", HomeCommand.teleportTarget("home Base-1").orElseThrow());
    }

    @Test
    void refusesAnythingThatIsNotExactlyOneTeleport() {
        assertTrue(HomeCommand.teleportTarget("home base extra").isEmpty());
        assertTrue(HomeCommand.teleportTarget("home").isEmpty());
        assertTrue(HomeCommand.teleportTarget("homebase").isEmpty());
        assertTrue(HomeCommand.teleportTarget("home base;op").isEmpty());
        assertTrue(HomeCommand.teleportTarget(null).isEmpty());
        assertFalse(HomeCommand.isTeleport("homes"));
    }

    @Test
    void refusesAnOverlongCommand() {
        String longName = "a".repeat(HomeNames.MAX_LENGTH + 40);
        assertTrue(HomeCommand.teleportTarget("/home " + longName).isEmpty());
    }

    @Test
    void buildsASafeOutgoingCommand() {
        assertEquals("home base", HomeCommand.teleport("base").orElseThrow());
        assertEquals("home Caf\u00E9", HomeCommand.teleport(" Caf\u00E9 ").orElseThrow());
    }

    @Test
    void neverBuildsACommandFromUnsafeInput() {
        assertTrue(HomeCommand.teleport("base\n/op me").isEmpty());
        assertTrue(HomeCommand.teleport("base extra").isEmpty());
        assertTrue(HomeCommand.teleport("").isEmpty());
        assertTrue(HomeCommand.teleport(null).isEmpty());
    }

    @Test
    void theRoundTripThroughTheTrustBoundaryIsLossless() {
        String name = "nether_hub-2";
        String command = HomeCommand.teleport(name).orElseThrow();
        assertEquals(name, HomeCommand.teleportTarget(command).orElseThrow());
    }
}
