package com.maxlananas.homegui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HomeListParser {
    private static final Pattern FORMATTING = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    private static final Pattern HEADING = Pattern.compile(
            "(?iu)(?:your|vos|deine|tus|suas|i tuoi|uw|twoje)?\\s*(?:homes?|maisons?|domiciles?)\\s*[:：>\\-]\\s*(.*)$");
    private static final Pattern BRACKETED = Pattern.compile("\\[([^]\\r\\n]+)]");
    private static final Pattern EMPTY = Pattern.compile(
            "(?iu)(?:no|aucun(?:e)?|keine|sin|nenhum(?:a)?)\\s+(?:homes?|maisons?|domiciles?)");

    public Optional<List<String>> parse(String raw) {
        if (raw == null || raw.length() > 16_384) return Optional.empty();
        String text = FORMATTING.matcher(raw).replaceAll("").strip();
        if (EMPTY.matcher(text).find()) return Optional.of(List.of());

        Matcher heading = HEADING.matcher(text);
        if (!heading.find()) return Optional.empty();
        String body = heading.group(1).strip();
        if (body.isEmpty()) return Optional.of(List.of());

        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        Matcher brackets = BRACKETED.matcher(body);
        while (brackets.find()) add(names, brackets.group(1));

        if (names.isEmpty()) {
            for (String candidate : body.split("(?:\\s*[,|;]\\s*|\\s+)", -1)) add(names, candidate);
        }
        return names.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(names.values()));
    }

    private static void add(LinkedHashMap<String, String> names, String candidate) {
        String cleaned = candidate.replaceAll("^[\\[({<]+|[\\])}>.,;]+$", "");
        HomeNames.validate(cleaned).ifPresent(name -> names.putIfAbsent(HomeNames.key(name), name));
    }
}
