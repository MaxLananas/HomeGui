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

    private static final Pattern DOT_RUN = Pattern.compile("\\.{2,}");
    private static final Pattern EDGE = Pattern.compile("^[.\\-]+|[.\\-]+$");

    /** Builds a key from a server address, falling back to {@link #LOCAL}. */
    public static String fromAddress(String address) {
        return sanitise(address);
    }

    /** True when {@code key} is already in normal form, so reads and writes agree. */
    public static boolean isValid(String key) {
        return key != null && !key.isEmpty() && key.equals(sanitise(key));
    }

    /**
     * Normalises an untrusted address or a key read from disk. Everything that is not
     * part of a host name or port is dropped, repeated dots collapse, and a key that
     * has nothing left in it becomes {@link #LOCAL} rather than an empty bucket name.
     */
    public static String sanitise(String key) {
        if (key == null) return LOCAL;
        String value = UNSAFE.matcher(key.strip().toLowerCase(Locale.ROOT)).replaceAll("");
        value = EDGE.matcher(DOT_RUN.matcher(value).replaceAll(".")).replaceAll("");
        if (value.isEmpty() || value.equals("localhost") || value.equals("127.0.0.1")) return LOCAL;
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }
}
