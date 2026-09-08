package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HomeCommandTest {
    @Test void validatesCommandsAtTheTrustBoundary() {
        assertEquals("Base-1", HomeCommand.teleportTarget("/home Base-1").orElseThrow());
        assertTrue(HomeCommand.teleportTarget("home base extra").isEmpty());
        assertTrue(HomeCommand.teleport("base\n/op me").isEmpty());
    }
}
