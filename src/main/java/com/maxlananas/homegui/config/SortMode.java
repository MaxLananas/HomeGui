package com.maxlananas.homegui.config;

import java.util.List;

public enum SortMode {
    DEFAULT("sort.default"),
    ALPHABETICAL("sort.alphabetical"),
    MOST_USED("sort.most_used"),
    RECENT("sort.recent"),
    FAVORITES_FIRST("sort.favorites_first");

    public final String langKey;

    SortMode(String langKey) { this.langKey = langKey; }

    public SortMode next() {
        SortMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static SortMode fromString(String s) {
        for (SortMode m : values()) if (m.name().equalsIgnoreCase(s)) return m;
        return DEFAULT;
    }

    /** Sorts {@code homes} in place according to this mode. */
    public void apply(List<String> homes) {
        switch (this) {
            case ALPHABETICAL -> homes.sort(String::compareToIgnoreCase);
            case MOST_USED -> {
                var cfg = ModConfig.getInstance();
                homes.sort((a, b) -> Integer.compare(cfg.getUseCount(b), cfg.getUseCount(a)));
            }
            case RECENT -> {
                var cfg = ModConfig.getInstance();
                var recentNames = new java.util.LinkedHashSet<String>();
                for (var e : cfg.getHistory()) recentNames.add(e.homeName.toLowerCase());
                var sorted = new java.util.ArrayList<String>();
                for (var n : recentNames) homes.stream().filter(h -> h.equalsIgnoreCase(n)).forEach(sorted::add);
                homes.stream().filter(h -> !sorted.contains(h)).forEach(sorted::add);
                homes.clear();
                homes.addAll(sorted);
            }
            case FAVORITES_FIRST -> {
                var cfg = ModConfig.getInstance();
                homes.sort((a, b) -> {
                    int fa = cfg.isFavorite(a) ? 0 : 1;
                    int fb = cfg.isFavorite(b) ? 0 : 1;
                    if (fa != fb) return fa - fb;
                    return a.compareToIgnoreCase(b);
                });
            }
            default -> { }
        }
    }
}
