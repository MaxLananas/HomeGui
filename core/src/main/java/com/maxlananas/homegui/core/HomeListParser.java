package com.maxlananas.homegui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns server chat into a list of homes.
 *
 * <p>Home plugins do not agree on a format, and several of them split one
 * {@code /homes} reply over many chat messages. The recogniser therefore works as a
 * small state machine fed line by line through {@link HomeListSession} instead of
 * trying to understand a whole reply in one string. {@link #parse(String)} is the
 * convenience wrapper for a reply that fits in a single message.
 *
 * <p>Nothing is guessed. A line only contributes a home when it is classified as a
 * heading or a structured entry, and every resulting name passes {@link HomeNames}.
 * Anything unrecognised is ignored, which is what keeps
 * {@code [Server] Welcome home, player!} out of the interface.
 *
 * <p>Recognised shapes include EssentialsX ({@code Homes: base, farm},
 * {@code Homes (3): base, farm}, paginated {@code ----- Homes (1/2) -----} followed
 * by {@code base - world: 100, 64, 200}) and CMI ({@code Homes:} on its own line
 * followed by {@code base (world) 100,64,200}, and
 * {@code Home: base | world | 100 64 200}), in every language HomeGui ships.
 */
public final class HomeListParser {

    /** Refuse to hold more homes than any plausible server can list. */
    public static final int MAX_HOMES = 4_096;
    private static final int MAX_LINE = 2_048;

    private static final Pattern FORMATTING = Pattern.compile("[\u00A7&][0-9A-FK-ORa-fk-or]");
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200F\\uFEFF\\u00A0]");
    private static final Pattern DIGITS = Pattern.compile("-?\\d+");

    private static final Pattern EMPTY_LIST = Pattern.compile(
            "(?iu)\\b(?:no|not\\s+any|any|aucun(?:e)?|keine|sin|ning[uú]n(?:a)?|nenhum(?:a)?"
                    + "|nessun(?:o|a)?|geen|brak|\u043d\u0435\u0442)\\s+"
                    + "(?:homes?|maisons?|domiciles?|dom(?:y|\u00f3w)?|hems?"
                    + "|ev(?:ler(?:im)?)?|\u0434\u043e\u043c(?:\u0430|\u044b)?|\u5bb6|\ud648|\u30db\u30fc\u30e0)\\b");

    private static final Pattern HEADING = Pattern.compile(
            "(?iu)^[-\u2013\u2014*>#\\s]*"
                    + "(?:(?:your|my|the|vos|mes|les|deine|meine|die|tus|mis|suas|minhas"
                    + "|i\\s+tuo?i|uw|mijn|twoje|moje|\u043c\u043e\u0438)\\s+)*"
                    + "(?:homes?|maisons?|domiciles?|dom(?:y|\u00f3w)?|hems?"
                    + "|ev(?:ler(?:im)?)?|\u0434\u043e\u043c(?:\u0430|\u044b)?"
                    + "|\u5bb6|\ud648|\u30db\u30fc\u30e0)"
                    + "\\s*(?:\\(\\s*\\d+\\s*(?:/\\s*(\\d+)\\s*)?\\))?"
                    // A separator is required, otherwise ordinary prose such as
                    // "HomeGui could not read that" would be read as a heading.
                    + "\\s*(?:[:\uff1a>\\-]|$)\\s*(.*)$");

    private static final Pattern PAGE_MARK = Pattern.compile(
            "(?iu)^[-\\s*>#]*(?:page|seite|pagina|\u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430)?\\s*"
                    + "\\(?\\s*\\d+\\s*(?:/|of|von|de|sur|z)\\s*\\d+\\s*\\)?[-\\s]*$");

    private static final Pattern LABELLED_PREFIX = Pattern.compile(
            "(?i)^\\s*(?:[-\u2022*>#]\\s*|\\d+\\s*[.)]\\s*|\\d+\\s*[-)\u2013]\\s*)?"
                    + "(?:home|maison|haus|casa|dom)\\s*[:\uff1a]\\s*");
    private static final Pattern LEADING_BULLET = Pattern.compile(
            "^\\s*(?:[-\u2022*>#]\\s+|\\d+\\s*[.)]\\s+|\\d+\\s*[-)\u2013]\\s+)");

    private static final Pattern WORLD_PAREN = Pattern.compile(
            "^\\s*[\\(\\[]\\s*([\\p{L}\\p{N}_.:\\-]{1,64})\\s*[\\)\\]]\\s*");
    private static final Pattern WORLD_COLON = Pattern.compile(
            "(?i)^\\s*[-\u2013\u2014|:]?\\s*(?:world|monde|welt|mundo|mondo|\u043c\u0438\u0440)\\s*[:\uff1a]\\s*"
                    + "([\\p{L}\\p{N}_.\\-]{1,64})\\s*");
    private static final Pattern WORLD_PIPE = Pattern.compile(
            "^\\s*[-\u2013\u2014|]\\s*([\\p{L}\\p{N}_.\\-]{1,64})\\s*(?=[-\u2013\u2014|]|$)");
    private static final Pattern COORDS = Pattern.compile(
            "(-?\\d{1,9})\\s*(?:[,;]|\\s)\\s*(-?\\d{1,9})\\s*(?:[,;]|\\s)\\s*(-?\\d{1,9})\\b");
    private static final Pattern BRACKETED = Pattern.compile("\\[([^]\\r\\n]{1,80})]");
    private static final Pattern WHOLE_BRACKET = Pattern.compile("^\\s*[\\[\\(]([^\\]\\)]{1,80})[\\]\\)]\\s*$");
    private static final Pattern TRAILING = Pattern.compile("[\\])}>,;.:]+$");

    public Optional<List<Home>> parse(String raw) {
        HomeListSession session = newSession();
        Optional<List<Home>> immediate = session.offer(raw);
        return immediate.isPresent() ? immediate : session.finish();
    }

    public HomeListSession newSession() {
        return new HomeListSession();
    }

    /** Cleans colour codes, invisible characters and surrounding whitespace. */
    public static String normalise(String raw) {
        if (raw == null) return "";
        String text = FORMATTING.matcher(raw).replaceAll("");
        text = INVISIBLE.matcher(text).replaceAll(" ");
        return text.strip();
    }

    /** Classifies one chat line. Exposed so tests can pin the recogniser down. */
    public static LineKind classify(String raw) {
        String text = normalise(raw);
        if (text.isEmpty()) return LineKind.BLANK;
        if (text.length() > MAX_LINE) return LineKind.OTHER;
        if (EMPTY_LIST.matcher(text).find()) return LineKind.EMPTY_LIST;
        if (PAGE_MARK.matcher(text).matches()) return LineKind.PAGE_MARK;
        // Structured first, and labelled lines before the heading check, so that
        // CMI's "Home: base" is not read as a heading called "Home".
        if (structuredEntry(text).isPresent()) return LineKind.ENTRY;
        if (isLabelled(text)) {
            String rest = LABELLED_PREFIX.matcher(text).replaceAll("");
            if (rest.isEmpty()) return LineKind.HEADING;
            return bareName(rest).isPresent() ? LineKind.BARE_NAME : LineKind.OTHER;
        }
        if (HEADING.matcher(text).matches()) return LineKind.HEADING;
        if (bareName(text).isPresent()) return LineKind.BARE_NAME;
        return LineKind.OTHER;
    }

    /**
     * Parses one line as a list entry.
     *
     * @param allowBare when false, a line that is only a bare word is rejected. That
     *                  guard stops ordinary chat from being collected as homes while
     *                  a reply is still expected.
     */
    static Optional<Home> parseEntry(String text, boolean allowBare) {
        Optional<Home> structured = structuredEntry(text);
        if (structured.isPresent()) return structured;
        if (!allowBare) return Optional.empty();
        return bareName(text).map(Home::of);
    }

    /** True for lines a home plugin labels, such as CMI's {@code Home: base}. */
    static boolean isLabelled(String text) {
        return LABELLED_PREFIX.matcher(text).lookingAt();
    }

    /** Parses the inline body of a heading such as {@code base, farm | mine}. */
    static List<Home> inlineNames(String body) {
        List<Home> homes = new ArrayList<>();
        if (body == null) return homes;
        String text = body.strip();
        if (text.isEmpty()) return homes;

        Matcher brackets = BRACKETED.matcher(text);
        boolean sawBracket = false;
        while (brackets.find()) {
            sawBracket = true;
            add(homes, brackets.group(1));
        }
        if (sawBracket) return homes;

        // Punctuation only. Splitting on whitespace as well would turn a sentence that
        // happens to follow a heading into a list of one word homes.
        for (String candidate : text.split("\\s*[,|;]\\s*", -1)) {
            add(homes, candidate);
        }
        return homes;
    }

    /** Parses an entry line that carries a world, coordinates, or both. */
    static Optional<Home> structuredEntry(String rawText) {
        String text = stripPrefixes(rawText);
        if (text.isEmpty()) return Optional.empty();

        int split = nameEnd(text);
        Optional<String> name = HomeNames.validate(cleanToken(text.substring(0, split)));
        if (name.isEmpty()) return Optional.empty();
        String rest = text.substring(split);

        String world = null;
        Matcher paren = WORLD_PAREN.matcher(rest);
        Matcher colon = WORLD_COLON.matcher(rest);
        Matcher pipe = WORLD_PIPE.matcher(rest);
        // A parenthesised number is EssentialsX's home count ("Homes (3): ..."), not a
        // world name, so it must not turn the heading into an entry called "Homes".
        if (paren.lookingAt() && !isNumber(paren.group(1))) {
            world = paren.group(1);
            rest = rest.substring(paren.end());
        } else if (colon.lookingAt() && !isNumber(colon.group(1))) {
            world = colon.group(1);
            rest = rest.substring(colon.end());
        } else if (pipe.lookingAt() && !isNumber(pipe.group(1))) {
            world = pipe.group(1);
            rest = rest.substring(pipe.end());
        }

        Home.Coordinates coordinates = null;
        Matcher coords = COORDS.matcher(rest);
        if (coords.find()) {
            coordinates = new Home.Coordinates(
                    Integer.parseInt(coords.group(1)),
                    Integer.parseInt(coords.group(2)),
                    Integer.parseInt(coords.group(3)));
        }

        if (world == null && coordinates == null) return Optional.empty();
        return Optional.of(new Home(name.get(), world, coordinates));
    }

    /** Parses a line that is nothing but a home name, possibly bulleted or bracketed. */
    static Optional<String> bareName(String rawText) {
        String text = LEADING_BULLET.matcher(stripPrefixes(rawText)).replaceAll("");
        Matcher brackets = WHOLE_BRACKET.matcher(text);
        if (brackets.matches()) text = brackets.group(1);
        return HomeNames.validate(cleanToken(text));
    }

    private static String stripPrefixes(String rawText) {
        String text = LABELLED_PREFIX.matcher(rawText).replaceAll("");
        return LEADING_BULLET.matcher(text).replaceAll("").strip();
    }

    /** Index just past the home name at the start of {@code text}. */
    private static int nameEnd(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '|' || c == ';' || c == ',') return i;
            if (i > 0 && (c == '(' || c == '[' || c == '<')) return i;
        }
        return text.length();
    }

    private static boolean isNumber(String value) {
        return DIGITS.matcher(value).matches();
    }

    private static String cleanToken(String candidate) {
        if (candidate == null) return "";
        String value = candidate.strip();
        value = value.replaceAll("^[\\[({<\u00a7&]+", "");
        value = TRAILING.matcher(value).replaceAll("");
        return value.strip();
    }

    private static void add(List<Home> homes, String candidate) {
        HomeNames.validate(cleanToken(candidate))
                .filter(name -> homes.size() < MAX_HOMES && !contains(homes, name))
                .ifPresent(name -> homes.add(Home.of(name)));
    }

    private static boolean contains(List<Home> homes, String name) {
        String key = HomeNames.key(name);
        for (Home home : homes) {
            if (home.key().equals(key)) return true;
        }
        return false;
    }

    /** What a single chat line turned out to be. */
    public enum LineKind {
        BLANK,
        EMPTY_LIST,
        HEADING,
        ENTRY,
        BARE_NAME,
        PAGE_MARK,
        OTHER
    }

    /**
     * Accumulates the lines of one {@code /homes} reply.
     *
     * <p>A session completes as soon as a self-contained heading is seen
     * ({@code Homes: base, farm}). For multi-message replies it stays open and
     * completes when a line that cannot belong to the list arrives, or when
     * {@link #finish()} is called because the request timed out.
     */
    public static final class HomeListSession {

        private final LinkedHashMap<String, Home> homes = new LinkedHashMap<>();
        private boolean headingSeen;
        private boolean closed;
        private int linesSeen;
        private int unrecognised;

        HomeListSession() {}

        /**
         * @return the finished list when this line completed the reply, otherwise
         *         empty, meaning "keep waiting".
         */
        public Optional<List<Home>> offer(String raw) {
            if (closed) return Optional.empty();
            String text = normalise(raw);
            linesSeen++;
            if (text.isEmpty() || text.length() > MAX_LINE) return Optional.empty();
            // "Home: base" is home output, so from here on a bare word is a home name.
            if (isLabelled(text)) headingSeen = true;

            switch (classify(text)) {
                case EMPTY_LIST:
                    homes.clear();
                    return close();
                case BLANK:
                case PAGE_MARK:
                    return Optional.empty();
                case HEADING:
                    return onHeading(text);
                case ENTRY:
                    parseEntry(text, true).ifPresent(this::put);
                    return Optional.empty();
                case BARE_NAME:
                    // A lone word is only a home once a heading said a list started;
                    // otherwise ordinary chat would be collected as homes.
                    if (!headingSeen) {
                        unrecognised++;
                        return Optional.empty();
                    }
                    parseEntry(text, true).ifPresent(this::put);
                    return Optional.empty();
                case OTHER:
                default:
                    if (headingSeen && !homes.isEmpty()) return close();
                    unrecognised++;
                    return Optional.empty();
            }
        }

        /**
         * Completes the session. Called when the request times out, when the player
         * disconnects, or when the caller decides the reply is over.
         */
        public Optional<List<Home>> finish() {
            if (closed) return Optional.of(result());
            closed = true;
            return homes.isEmpty() ? Optional.empty() : Optional.of(result());
        }

        private Optional<List<Home>> onHeading(String text) {
            Matcher matcher = HEADING.matcher(text);
            if (!matcher.matches()) return Optional.empty();
            headingSeen = true;
            boolean paginated = matcher.group(1) != null;
            List<Home> inline = inlineNames(matcher.group(2));
            for (Home home : inline) put(home);
            // "Homes: base, farm" is complete on its own. "Homes:" or a paginated
            // heading announces lines that follow in later messages.
            return !paginated && !inline.isEmpty() ? close() : Optional.empty();
        }

        private void put(Home home) {
            Home existing = homes.get(home.key());
            if (existing != null && !home.hasCoordinates() && !home.hasWorld()) return;
            if (existing == null && homes.size() >= MAX_HOMES) return;
            homes.put(home.key(), home);
        }

        private Optional<List<Home>> close() {
            closed = true;
            return Optional.of(result());
        }

        private List<Home> result() {
            return Collections.unmodifiableList(new ArrayList<>(homes.values()));
        }

        public boolean isClosed() { return closed; }
        public boolean sawHeading() { return headingSeen; }
        public int linesSeen() { return linesSeen; }
        public int unrecognisedLines() { return unrecognised; }
        public int size() { return homes.size(); }
    }
}
