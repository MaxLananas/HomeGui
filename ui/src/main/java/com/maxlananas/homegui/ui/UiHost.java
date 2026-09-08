package com.maxlananas.homegui.ui;

/**
 * What the interface asks of the running game.
 *
 * <p>Implemented by each loader bridge. Everything version specific lives behind
 * these seven methods, so the interface itself never has to know which loader or
 * Minecraft generation it is running on.
 */
public interface UiHost {

    /** Closes the interface. */
    void close();

    /** Sends {@code /homes} again. */
    void refresh();

    /**
     * Asks the narrator to speak a message, used for feedback that has no visible
     * focus change, such as a completed export.
     */
    void announce(String message);

    /** Tells the bridge that the element set changed and widgets must be rebuilt. */
    void invalidate();

    /** Milliseconds since the epoch, so animations can be driven from one clock. */
    long now();

    /**
     * Called when the interface produces user visible feedback, so the bridge can
     * also mirror it into the action bar or chat when that is more appropriate.
     */
    default void feedback(String message) {}
}
