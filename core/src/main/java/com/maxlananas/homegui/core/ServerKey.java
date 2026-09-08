package com.maxlananas.homegui.core;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identifies the server the player is currently on so that favourites, statistics,
 * history and captured coordinates are not shared between unrelated servers.
 *
 * <p>Two players on the same address get the same key; the value never contains
 * credentials, only host, port or level name.
 */
public final class ServerKey {

    /** Used for single player and for any moment when no server is known. */
    public static final String LOCAL = "local";

    private static final Pattern UNSAFE = Pattern.compile("[^a-z0-9._:\\-]");
    private static final int MAX_LENGTH = 160;

    private ServerKey() {}

    /** Builds a key from a server address, falling back to {@link #LOCAL}. */
    public static String fromAddress(String address) {
        if (address == null) return LOCAL;
        String value = UNSAFE.matcher(address.strip().toLowerCase(Locale.ROOT)).replaceAll("");
        if (value.isEmpty() || value.equals("localhost") || value.equals("127.0.0.1")) return LOCAL;
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }

    /** True when {@code key} is a value this class could have produced. */
    public static boolean isValid(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_LENGTH) return false;
        return LOCAL.equals(key) || !UNSAFE.matcher(key).find();
    }

    /** Normalises an untrusted key read from disk back into a safe one. */
    public static String sanitise(String key) {
        return isValid(key) ? key : LOCAL;
    }
}
