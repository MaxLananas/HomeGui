package com.maxlananas.homegui.core;

import java.util.Locale;

/**
 * Every setting HomeGui persists, with the validation applied when it is read back.
 *
 * <p>Each option here has to change something visible; the interface reads them all.
 */
public final class Preferences {

    /** Row and card sizing. */
    public enum Density {
        COMPACT("homegui.density.compact"),
        COMFORTABLE("homegui.density.comfortable");

        public final String langKey;

        Density(String langKey) {
            this.langKey = langKey;
        }

        public Density next() {
            return this == COMPACT ? COMFORTABLE : COMPACT;
        }

        public static Density fromString(String value) {
            return value != null && COMPACT.name().equalsIgnoreCase(value) ? COMPACT : COMFORTABLE;
        }
    }

    /** Row layout for the home browser. */
    public enum ViewMode {
        LIST("homegui.view.list"),
        GRID("homegui.view.grid");

        public final String langKey;

        ViewMode(String langKey) {
            this.langKey = langKey;
        }

        public ViewMode next() {
            return this == LIST ? GRID : LIST;
        }

        public static ViewMode fromString(String value) {
            return value != null && GRID.name().equalsIgnoreCase(value) ? GRID : LIST;
        }
    }

    public static final int THEME_COUNT = 4;
    public static final int MIN_GRID_COLUMNS = 2;
    public static final int MAX_GRID_COLUMNS = 6;

    private String language = "en";
    private int themeIndex;
    private Density density = Density.COMFORTABLE;
    private ViewMode viewMode = ViewMode.LIST;
    private SortMode sortMode = SortMode.DEFAULT;
    private boolean transparentMenu;
    private boolean showCoordinates = true;
    private boolean showUseCounts = true;
    private boolean animationsEnabled = true;
    private boolean confirmDestructive = true;
    private int gridColumns = 3;

    public String language() { return language; }
    public int themeIndex() { return themeIndex; }
    public Density density() { return density; }
    public ViewMode viewMode() { return viewMode; }
    public SortMode sortMode() { return sortMode; }
    public boolean transparentMenu() { return transparentMenu; }
    public boolean showCoordinates() { return showCoordinates; }
    public boolean showUseCounts() { return showUseCounts; }
    public boolean animationsEnabled() { return animationsEnabled; }
    public boolean confirmDestructive() { return confirmDestructive; }
    public int gridColumns() { return gridColumns; }

    public void setLanguage(String value) {
        language = value != null && value.matches("[a-z]{2}(?:_[a-z]{2})?")
                ? value.toLowerCase(Locale.ROOT) : "en";
    }

    public void setThemeIndex(int value) {
        themeIndex = Math.max(0, Math.min(THEME_COUNT - 1, value));
    }

    public void cycleTheme() {
        setThemeIndex(themeIndex + 1);
    }

    public void setDensity(Density value) {
        density = value == null ? Density.COMFORTABLE : value;
    }

    public void setViewMode(ViewMode value) {
        viewMode = value == null ? ViewMode.LIST : value;
    }

    public void setSortMode(SortMode value) {
        sortMode = value == null ? SortMode.DEFAULT : value;
    }

    public void setTransparentMenu(boolean value) {
        transparentMenu = value;
    }

    public void setShowCoordinates(boolean value) {
        showCoordinates = value;
    }

    public void setShowUseCounts(boolean value) {
        showUseCounts = value;
    }

    public void setAnimationsEnabled(boolean value) {
        animationsEnabled = value;
    }

    public void setConfirmDestructive(boolean value) {
        confirmDestructive = value;
    }

    public void setGridColumns(int value) {
        gridColumns = Math.max(MIN_GRID_COLUMNS, Math.min(MAX_GRID_COLUMNS, value));
    }

    /** Row height in GUI pixels for the current density. */
    public int rowHeight() {
        return density == Density.COMPACT ? 18 : 24;
    }

    /** Card height in GUI pixels for the current density. */
    public int cardHeight() {
        return density == Density.COMPACT ? 40 : 52;
    }

    public Preferences copy() {
        Preferences copy = new Preferences();
        copy.language = language;
        copy.themeIndex = themeIndex;
        copy.density = density;
        copy.viewMode = viewMode;
        copy.sortMode = sortMode;
        copy.transparentMenu = transparentMenu;
        copy.showCoordinates = showCoordinates;
        copy.showUseCounts = showUseCounts;
        copy.animationsEnabled = animationsEnabled;
        copy.confirmDestructive = confirmDestructive;
        copy.gridColumns = gridColumns;
        return copy;
    }
}
