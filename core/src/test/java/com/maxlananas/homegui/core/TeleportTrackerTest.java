package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportTrackerTest {

    private static final class Recording implements TeleportTracker.Observer {
        final List<String> confirmed = new ArrayList<>();
        final List<String> unconfirmed = new ArrayList<>();

        @Override
        public void onConfirmed(String home, Position at, long now) { confirmed.add(home); }

        @Override
        public void onUnconfirmed(String home, long now) { unconfirmed.add(home); }
    }

    private final TeleportTracker tracker = new TeleportTracker();
    private final Recording observer = new Recording();

    @Test
    void nothingHappensWithoutAPendingAttempt() {
        tracker.observe(new Position(900, 70, 900, "overworld"), 0L, observer);
        assertTrue(observer.confirmed.isEmpty());
        assertTrue(observer.unconfirmed.isEmpty());
    }

    @Test
    void anUnknownOriginMeansNothingCanBeConfirmed() {
        tracker.arm("base", null, 0L);
        assertFalse(tracker.isPending());
    }

    @Test
    void movementBeyondTheThresholdConfirms() {
        tracker.arm("base", new Position(0, 64, 0, "overworld"), 0L);
        tracker.observe(new Position(TeleportTracker.MIN_DISTANCE + 1, 64, 0, "overworld"), 100L, observer);
        assertEquals(List.of("base"), observer.confirmed);
        assertFalse(tracker.isPending());
    }

    @Test
    void smallMovementNeverConfirmsAndEventuallyExpires() {
        tracker.arm("base", new Position(0, 64, 0, "overworld"), 0L);
        tracker.observe(new Position(1, 64, 0, "overworld"), 100L, observer);
        assertTrue(observer.confirmed.isEmpty());
        tracker.observe(new Position(1, 64, 0, "overworld"),
                TeleportTracker.CONFIRM_WINDOW_MILLIS, observer);
        assertEquals(List.of("base"), observer.unconfirmed);
    }

    @Test
    void cancelDropsTheAttemptSilently() {
        tracker.arm("base", new Position(0, 64, 0, "overworld"), 0L);
        tracker.cancel();
        assertFalse(tracker.isPending());
        tracker.observe(new Position(900, 70, 900, "overworld"), 10L, observer);
        assertTrue(observer.confirmed.isEmpty());
        assertTrue(observer.unconfirmed.isEmpty());
    }

    @Test
    void onlyTheFirstOutcomeIsReported() {
        tracker.arm("base", new Position(0, 64, 0, "overworld"), 0L);
        tracker.observe(new Position(900, 70, 900, "overworld"), 10L, observer);
        tracker.observe(new Position(1200, 70, 1200, "overworld"), 20L, observer);
        assertEquals(1, observer.confirmed.size());
    }
}
