package com.maxlananas.homegui.core;

import java.util.Collections;
import java.util.List;

/**
 * An immutable snapshot of the last {@code /homes} request and its outcome.
 *
 * <p>Snapshots are replaced rather than mutated, and {@link #revision()} changes on
 * every replacement, so an open screen can poll cheaply from its tick loop and only
 * rebuild when something actually happened.
 */
public final class HomeRequest {

    private final RequestState state;
    private final List<Home> homes;
    private final long updatedAt;
    private final int linesSeen;
    private final String serverKey;
    private final long revision;

    private HomeRequest(RequestState state, List<Home> homes, long updatedAt,
                        int linesSeen, String serverKey, long revision) {
        this.state = state;
        this.homes = homes;
        this.updatedAt = updatedAt;
        this.linesSeen = linesSeen;
        this.serverKey = serverKey;
        this.revision = revision;
    }

    public static HomeRequest idle(String serverKey, long now, long revision) {
        return new HomeRequest(RequestState.IDLE, Collections.emptyList(), now, 0, serverKey, revision);
    }

    public HomeRequest with(RequestState newState, List<Home> newHomes, long now, int seen, long newRevision) {
        return new HomeRequest(newState, newHomes, now, seen, serverKey, newRevision);
    }

    public HomeRequest forServer(String newServerKey, long now, long newRevision) {
        return new HomeRequest(RequestState.IDLE, Collections.emptyList(), now, 0, newServerKey, newRevision);
    }

    public RequestState state() { return state; }

    public List<Home> homes() { return homes; }

    public int homeCount() { return homes.size(); }

    public long updatedAt() { return updatedAt; }

    public int linesSeen() { return linesSeen; }

    public String serverKey() { return serverKey; }

    public long revision() { return revision; }

    /** True while the interface should show a spinner instead of a list. */
    public boolean isLoading() { return state == RequestState.LOADING; }

    /** True when there is a usable list to show, even if a refresh is in flight. */
    public boolean hasHomes() { return !homes.isEmpty(); }
}
