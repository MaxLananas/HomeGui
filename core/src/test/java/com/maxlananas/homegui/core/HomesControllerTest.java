package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomesControllerTest {

    private static final class FakeSender implements HomesController.CommandSender {
        final List<String> sent = new ArrayList<>();
        boolean refuses;

        @Override
        public boolean send(String command) {
            if (refuses) return false;
            sent.add(command);
            return true;
        }
    }

    private static final class Recording implements HomesController.Listener {
        final List<String> attempted = new ArrayList<>();
        final List<String> confirmed = new ArrayList<>();
        final List<String> unconfirmed = new ArrayList<>();
        Position destination;
        int changes;

        @Override
        public void onRequestChanged(HomeRequest request) { changes++; }

        @Override
        public void onTeleportAttempted(String home, long now) { attempted.add(home); }

        @Override
        public void onTeleportConfirmed(String home, Position at, long now) {
            confirmed.add(home);
            destination = at;
        }

        @Override
        public void onTeleportUnconfirmed(String home, long now) { unconfirmed.add(home); }
    }

    private final FakeSender sender = new FakeSender();
    private final long[] clock = {1_000_000L};
    private final HomesController controller =
            new HomesController(sender, () -> clock[0], 6_000L);
    private final Recording listener = new Recording();

    HomesControllerTest() {
        controller.setListener(listener);
    }

    private void advance(long millis) {
        clock[0] += millis;
        controller.tick();
    }

    private void receiveList() {
        controller.requestHomes();
        controller.onChatLine("Homes: base, farm, mine");
    }

    @Test
    void sendsTheListCommandAndReportsLoading() {
        assertTrue(controller.requestHomes());
        assertEquals(List.of("homes"), sender.sent);
        assertEquals(RequestState.LOADING, controller.request().state());
    }

    @Test
    void reportsFailureWhenTheCommandCouldNotBeSent() {
        sender.refuses = true;
        assertFalse(controller.requestHomes());
        assertEquals(RequestState.FAILED, controller.request().state());
    }

    @Test
    void completesAsSoonAsASingleMessageReplyArrives() {
        receiveList();
        assertEquals(RequestState.READY, controller.request().state());
        assertEquals(3, controller.homes().size());
        assertFalse(controller.isWaiting());
    }

    @Test
    void accumulatesAReplySpreadOverSeveralMessages() {
        controller.requestHomes();
        controller.onChatLine("Homes:");
        controller.onChatLine("base (world) 100, 64, 200");
        assertTrue(controller.isWaiting(), "the reply is not finished yet");
        controller.onChatLine("farm (world) 4, 5, 6");
        advance(7_000);
        assertEquals(RequestState.READY, controller.request().state());
        assertEquals(2, controller.homes().size());
    }

    @Test
    void reportsATimeoutWhenNothingArrives() {
        controller.requestHomes();
        advance(5_999);
        assertEquals(RequestState.LOADING, controller.request().state());
        advance(2);
        assertEquals(RequestState.TIMEOUT, controller.request().state());
    }

    @Test
    void distinguishesATimeoutFromAnUnreadableReply() {
        controller.requestHomes();
        controller.onChatLine("<Steve> nice build everyone");
        advance(7_000);
        assertEquals(RequestState.UNRECOGNISED, controller.request().state());
    }

    @Test
    void keepsThePreviousListVisibleWhileRefreshing() {
        receiveList();
        controller.requestHomes();
        assertEquals(RequestState.LOADING, controller.request().state());
        assertEquals(3, controller.homes().size(), "the last good list stays on screen");
    }

    @Test
    void ignoresChatThatArrivesOutsideARequest() {
        controller.onChatLine("Homes: base");
        assertTrue(controller.homes().isEmpty());
        assertEquals(RequestState.IDLE, controller.request().state());
    }

    @Test
    void clearingTheListOnDisconnectRemovesStaleHomes() {
        receiveList();
        controller.onDisconnected();
        assertEquals(RequestState.DISCONNECTED, controller.request().state());
        assertTrue(controller.homes().isEmpty());
    }

    @Test
    void changingServerDiscardsThePreviousServersHomes() {
        receiveList();
        controller.setServer("other.example.net");
        assertTrue(controller.homes().isEmpty());
        assertEquals("other.example.net", controller.request().serverKey());
        assertEquals(RequestState.IDLE, controller.request().state());
    }

    @Test
    void changingServerCancelsAnInFlightRequest() {
        controller.requestHomes();
        controller.setServer("other.example.net");
        assertFalse(controller.isWaiting());
        controller.onChatLine("Homes: base");
        assertTrue(controller.homes().isEmpty(), "a late reply from the old server is ignored");
    }

    @Test
    void everyStateChangePublishesANewRevision() {
        long before = controller.request().revision();
        controller.requestHomes();
        long during = controller.request().revision();
        controller.onChatLine("Homes: base");
        assertTrue(during > before);
        assertTrue(controller.request().revision() > during);
    }

    @Test
    void teleportRefusesANameThatDidNotComeFromTheServer() {
        assertFalse(controller.teleport("base\n/op me"));
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    void teleportSendsASafeCommand() {
        assertTrue(controller.teleport("Caf\u00E9"));
        assertEquals(List.of("home Caf\u00E9"), sender.sent);
    }

    @Test
    void aTeleportIsOnlyCountedOnceThePlayerActuallyMoves() {
        Position start = new Position(10, 64, 10, "overworld");
        controller.onCommandSent("home base", start);
        assertEquals(List.of("base"), listener.attempted);
        assertTrue(listener.confirmed.isEmpty());

        controller.onPlayerPosition(new Position(10.4, 64, 10.2, "overworld"));
        assertTrue(listener.confirmed.isEmpty(), "a step is not a teleport");

        controller.onPlayerPosition(new Position(500, 70, -300, "overworld"));
        assertEquals(List.of("base"), listener.confirmed);
        assertEquals(500.0, listener.destination.x);
    }

    @Test
    void aRefusedTeleportIsReportedAndNeverCounted() {
        Position start = new Position(10, 64, 10, "overworld");
        controller.onCommandSent("home nowhere", start);
        advance(7_000);
        controller.onPlayerPosition(start);
        assertEquals(List.of("nowhere"), listener.unconfirmed);
        assertTrue(listener.confirmed.isEmpty());
    }

    @Test
    void crossingADimensionCountsAsATeleport() {
        controller.onCommandSent("home nether_hub", new Position(0, 64, 0, "overworld"));
        controller.onPlayerPosition(new Position(0, 64, 0, "the_nether"));
        assertEquals(List.of("nether_hub"), listener.confirmed);
    }

    @Test
    void aHandTypedTeleportIsCanonicalisedAgainstTheServerList() {
        receiveList();
        controller.onCommandSent("home BASE", new Position(0, 64, 0, "overworld"));
        assertEquals(List.of("base"), listener.attempted, "the server's own casing is used");
    }

    @Test
    void aDisconnectCancelsAPendingTeleport() {
        controller.onCommandSent("home base", new Position(0, 64, 0, "overworld"));
        assertTrue(controller.hasPendingTeleport());
        controller.onDisconnected();
        assertFalse(controller.hasPendingTeleport());
        advance(7_000);
        controller.onPlayerPosition(new Position(900, 70, 900, "overworld"));
        assertTrue(listener.confirmed.isEmpty());
    }
}
