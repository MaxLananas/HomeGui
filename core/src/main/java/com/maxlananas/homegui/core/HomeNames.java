package com.maxlananas.homegui.core;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class HomeNames {
    private static final Pattern SAFE = Pattern.compile("[\\p{L}\\p{N}_.:@+\\-]{1,64}");

    private HomeNames() {}

    public static Optional<String> validate(String value) {
        if (value == null) return Optional.empty();
        String name = value.strip();
        return SAFE.matcher(name).matches() ? Optional.of(name) : Optional.empty();
    }

    public static String key(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
