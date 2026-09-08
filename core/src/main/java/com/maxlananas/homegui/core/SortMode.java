package com.maxlananas.homegui.core;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The sort orders the interface offers.
 *
 * <p>Lives in the core so that ordering is identical on every loader and version and
 * can be tested without a client. Data about the homes themselves is read through
 * {@link HomeIndex}, which keeps this enum free of any storage or Minecraft type.
 */
public enum SortMode {

    /** Keeps the order the server printed, which is often the order it stores. */
    DEFAULT("homegui.sort.default"),

    ALPHABETICAL("homegui.sort.alphabetical"),

    MOST_USED("homegui.sort.most_used"),

    RECENT("homegui.sort.recent"),

    FAVORITES_FIRST("homegui.sort.favorites_first");

    public final String langKey;

    SortMode(String langKey) {
        this.langKey = langKey;
    }

    public SortMode next() {
        SortMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static SortMode fromString(String value) {
        if (value != null) {
            for (SortMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) return mode;
            }
        }
        return DEFAULT;
    }

    /** Sorts {@code homes} in place. */
    public void apply(List<Home> homes, HomeIndex index) {
        homes.sort(comparator(index));
    }

    /** A stable comparator, so equal homes keep the server's order. */
    public Comparator<Home> comparator(HomeIndex index) {
        switch (this) {
            case ALPHABETICAL:
                return (a, b) -> a.name().compareToIgnoreCase(b.name());
            case MOST_USED:
                return Comparator
                        .comparingInt((Home home) -> index.useCount(home.name())).reversed()
                        .thenComparing(Comparator.comparing((Home home) -> home.name(), String.CASE_INSENSITIVE_ORDER));
            case RECENT:
                return Comparator
                        .comparingInt((Home home) -> index.recencyRank(home.name()))
                        .thenComparing(Comparator.comparing((Home home) -> home.name(), String.CASE_INSENSITIVE_ORDER));
            case FAVORITES_FIRST:
                return Comparator
                        .comparingInt((Home home) -> index.isFavorite(home.name()) ? 0 : 1)
                        .thenComparing(Comparator.comparing((Home home) -> home.name(), String.CASE_INSENSITIVE_ORDER));
            case DEFAULT:
            default:
                return (a, b) -> 0;
        }
    }

    /** Localised display name in lower case, used for stable alphabetical order. */
    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Read-only view of the local data a sort order needs. */
    public interface HomeIndex {
        boolean isFavorite(String home);

        int useCount(String home);

        /** 0 for the most recently used home, {@link Integer#MAX_VALUE} when unknown. */
        int recencyRank(String home);
    }
}
