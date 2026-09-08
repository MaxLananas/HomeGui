package com.maxlananas.homegui.core;

/**
 * Decides whether a {@code /home} command actually teleported the player.
 *
 * <p>HomeGui only sees the command it sent; the server is free to refuse it
 * (unknown home, cooldown, no permission, wrong world). Counting the attempt rather
 * than the outcome is what used to inflate statistics, so a teleport is only
 * confirmed when the player's position or dimension actually changes inside a short
 * window after the command.
 *
 * <p>Known limitation, documented rather than hidden: teleporting to a home the
 * player is already standing at produces no movement and is therefore reported as
 * unconfirmed. That is the price of a client-only mod with no server component.
 */
public final class TeleportTracker {

    /** How long HomeGui waits for movement before giving up on an attempt. */
    public static final long CONFIRM_WINDOW_MILLIS = 6_000L;

    /** Movement, in blocks, that counts as a real teleport. */
    public static final double MIN_DISTANCE = 3.0D;

    /** Receives the outcome of an attempt. */
    public interface Observer {
        /** The player moved: the teleport happened and {@code at} is the destination. */
        void onConfirmed(String home, Position at, long now);

        /** The window closed without movement: nothing is counted. */
        void onUnconfirmed(String home, long now);
    }

    private String pendingHome;
    private Position origin;
    private long startedAt;

    /**
     * Starts watching for the effect of a freshly sent {@code /home} command.
     * Without a known origin there is nothing to compare against, so no attempt is
     * tracked and no statistic is counted.
     */
    public void arm(String home, Position origin, long now) {
        if (origin == null) {
            clear();
            return;
        }
        this.pendingHome = home;
        this.origin = origin;
        this.startedAt = now;
    }

    /** Feeds a position sample. Safe to call when nothing is pending. */
    public void observe(Position position, long now, Observer observer) {
        if (pendingHome == null || position == null) return;
        boolean moved = !origin.sameDimension(position) || origin.distanceTo(position) >= MIN_DISTANCE;
        if (moved) {
            String home = pendingHome;
            clear();
            observer.onConfirmed(home, position, now);
            return;
        }
        if (now - startedAt >= CONFIRM_WINDOW_MILLIS) {
            String home = pendingHome;
            clear();
            observer.onUnconfirmed(home, now);
        }
    }

    /** Drops a pending attempt, for example when the player disconnects. */
    public void cancel() {
        clear();
    }

    public boolean isPending() { return pendingHome != null; }

    public String pendingHome() { return pendingHome; }

    private void clear() {
        pendingHome = null;
        origin = null;
        startedAt = 0L;
    }
}
