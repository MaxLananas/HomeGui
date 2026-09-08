package com.maxlananas.homegui.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * The whole request lifecycle, with no Minecraft in sight.
 *
 * <p>Sends {@code /homes}, feeds chat lines into {@link HomeListParser}, applies the
 * timeout, reacts to disconnects and server changes, and reports every transition
 * through {@link Listener}. Keeping it here means loading, timeout, multi-message
 * replies and teleport confirmation are all covered by ordinary unit tests instead
 * of by hand in a running client.
 */
public final class HomesController {

    /** Default wait for a server reply before the interface reports a timeout. */
    public static final long DEFAULT_TIMEOUT_MILLIS = 6_000L;

    /** Sends a command to the server. Returns false when nothing could be sent. */
    public interface CommandSender {
        boolean send(String command);
    }

    /** Notified about every state change and every teleport outcome. */
    public interface Listener {
        default void onRequestChanged(HomeRequest request) {}
        default void onTeleportAttempted(String home, long now) {}
        default void onTeleportConfirmed(String home, Position destination, long now) {}
        default void onTeleportUnconfirmed(String home, long now) {}
    }

    private final CommandSender sender;
    private final LongSupplier clock;
    private final long timeoutMillis;
    private final HomeListParser parser = new HomeListParser();
    private final TeleportTracker tracker = new TeleportTracker();

    private Listener listener = new Listener() {};
    private HomeListParser.HomeListSession session;
    private HomeRequest request = HomeRequest.idle(ServerKey.LOCAL, 0L, 0L);
    private long revision;
    private boolean waiting;
    private long deadline;

    public HomesController(CommandSender sender) {
        this(sender, System::currentTimeMillis, DEFAULT_TIMEOUT_MILLIS);
    }

    public HomesController(CommandSender sender, LongSupplier clock, long timeoutMillis) {
        this.sender = sender;
        this.clock = clock;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    public void setListener(Listener listener) {
        this.listener = listener == null ? new Listener() {} : listener;
    }

    /**
     * Notes the server the player is on. Changing server discards the previous
     * list, which belonged to somebody else's plugin.
     */
    public void setServer(String serverKey) {
        String key = ServerKey.sanitise(serverKey);
        if (key.equals(request.serverKey()) && !waiting) return;
        waiting = false;
        session = null;
        tracker.cancel();
        if (!key.equals(request.serverKey())) {
            publish(request.forServer(key, now(), nextRevision()));
        } else {
            publish(request.with(RequestState.IDLE, Collections.emptyList(), now(), 0, nextRevision()));
        }
    }

    /** Asks the server for its home list. */
    public boolean requestHomes() {
        if (!sender.send(HomeCommand.LIST_COMMAND)) {
            waiting = false;
            publish(request.with(RequestState.FAILED, request.homes(), now(), 0, nextRevision()));
            return false;
        }
        session = parser.newSession();
        deadline = now() + timeoutMillis;
        waiting = true;
        publish(request.with(RequestState.LOADING, request.homes(), now(), 0, nextRevision()));
        return true;
    }

    /** Feeds one received chat line. Ignored unless a request is in flight. */
    public void onChatLine(String raw) {
        if (!waiting || session == null) return;
        Optional<List<Home>> parsed = session.offer(raw);
        parsed.ifPresent(this::complete);
    }

    /** Called for every command the client sends, GUI initiated or typed by hand. */
    public void onCommandSent(String command, Position origin) {
        Optional<String> target = HomeCommand.teleportTarget(command);
        if (target.isEmpty()) return;
        String home = canonicalName(target.get());
        tracker.arm(home, origin, now());
        listener.onTeleportAttempted(home, now());
    }

    /** Feeds a position sample so teleport confirmation can happen. */
    public void onPlayerPosition(Position position) {
        tracker.observe(position, now(), new TeleportTracker.Observer() {
            @Override
            public void onConfirmed(String home, Position at, long at0) {
                listener.onTeleportConfirmed(home, at, at0);
            }

            @Override
            public void onUnconfirmed(String home, long at0) {
                listener.onTeleportUnconfirmed(home, at0);
            }
        });
    }

    /** Drives the timeout. Call once per client tick. */
    public void tick() {
        if (!waiting) return;
        if (now() < deadline) return;
        Optional<List<Home>> partial = session == null ? Optional.empty() : session.finish();
        if (partial.isPresent()) {
            complete(partial.get());
            return;
        }
        waiting = false;
        int seen = session == null ? 0 : session.linesSeen();
        RequestState state = seen > 0 ? RequestState.UNRECOGNISED : RequestState.TIMEOUT;
        publish(request.with(state, request.homes(), now(), seen, nextRevision()));
    }

    /** The player left the world or switched server. */
    public void onDisconnected() {
        waiting = false;
        session = null;
        tracker.cancel();
        publish(request.with(RequestState.DISCONNECTED, Collections.emptyList(), now(), 0, nextRevision()));
    }

    /** Teleports, refusing anything that did not come back through the parser intact. */
    public boolean teleport(String home) {
        Optional<String> command = HomeCommand.teleport(home);
        if (command.isEmpty()) return false;
        return sender.send(command.get());
    }

    private void complete(List<Home> homes) {
        waiting = false;
        int seen = session == null ? 0 : session.linesSeen();
        RequestState state = homes.isEmpty() ? RequestState.EMPTY : RequestState.READY;
        publish(request.with(state, Collections.unmodifiableList(homes), now(), seen, nextRevision()));
    }

    /** The display name the server used, for a name the player may have re-typed. */
    public String canonicalName(String name) {
        String key = HomeNames.key(name);
        for (Home home : request.homes()) {
            if (home.key().equals(key)) return home.name();
        }
        return name;
    }

    public HomeRequest request() { return request; }

    public List<Home> homes() { return request.homes(); }

    public boolean isWaiting() { return waiting; }

    public boolean hasPendingTeleport() { return tracker.isPending(); }

    public long timeoutMillis() { return timeoutMillis; }

    private void publish(HomeRequest next) {
        request = next;
        listener.onRequestChanged(next);
    }

    private long now() { return clock.getAsLong(); }

    private long nextRevision() { return ++revision; }
}
