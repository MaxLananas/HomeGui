package com.maxlananas.homegui.core;

/** Where a {@code /homes} request currently stands. Drives the whole interface. */
public enum RequestState {

    /** Nothing has been asked yet, for example right after joining a server. */
    IDLE,

    /** {@code /homes} was sent and the reply has not been recognised yet. */
    LOADING,

    /** A reply was recognised and at least one home came back. */
    READY,

    /** The server answered and reported that the player has no homes. */
    EMPTY,

    /** The reply never arrived, or arrived without anything recognisable, before the timeout. */
    TIMEOUT,

    /** The server printed something, but no known home format was recognised. */
    UNRECOGNISED,

    /** The player left the server (or changed server) while a request was in flight. */
    DISCONNECTED,

    /** The command could not be sent at all, for example because there is no connection. */
    FAILED
}
