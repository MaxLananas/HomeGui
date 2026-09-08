package com.maxlananas.homegui.core;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The single trust boundary between text that comes from a server's chat and text
 * that HomeGui is willing to place inside a {@code /home} command.
 *
 * <p>A name is accepted when it is a single token of at most {@value #MAX_LENGTH}
 * characters that starts with a Unicode letter or digit and continues with letters,
 * digits and the handful of punctuation characters home plugins actually use.
 * Everything else (whitespace, control characters, command separators, path
 * separators, colour codes, runs of decoration such as {@code ---}) is rejected,
 * which makes command injection structurally impossible rather than merely filtered.
 */
public final class HomeNames {

    /** Longest name HomeGui will ever put in a command or persist. */
    public static final int MAX_LENGTH = 64;

    private static final Pattern SAFE =
            Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_.:@+\\-]{0," + (MAX_LENGTH - 1) + "}");
    private static final Pattern EDGE = Pattern.compile("^[\\p{Z}\\p{C}]+|[\\p{Z}\\p{C}]+$");

    private HomeNames() {}

    /**
     * @return the canonical form of {@code value} when it is safe to use, otherwise empty.
     */
    public static Optional<String> validate(String value) {
        if (value == null) return Optional.empty();
        String name = EDGE.matcher(value).replaceAll("");
        if (name.isEmpty() || name.length() > MAX_LENGTH) return Optional.empty();
        return SAFE.matcher(name).matches() ? Optional.of(name) : Optional.empty();
    }

    /** True when {@code value} is a name HomeGui would accept. */
    public static boolean isValid(String value) {
        return validate(value).isPresent();
    }

    /** Case-insensitive identity used for storage keys. Locale independent on purpose. */
    public static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /** Storage key for a name that has already been validated. */
    public static Optional<String> keyOf(String value) {
        return validate(value).map(HomeNames::key);
    }
}
