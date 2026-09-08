package com.maxlananas.homegui.ui;

import com.maxlananas.homegui.core.ConfigStore;
import com.maxlananas.homegui.core.Home;
import com.maxlananas.homegui.core.HomeNames;
import com.maxlananas.homegui.core.HomesController;
import com.maxlananas.homegui.core.Preferences;
import com.maxlananas.homegui.core.RequestState;
import com.maxlananas.homegui.core.SortMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The whole HomeGui interface.
 *
 * <p>Layout, navigation, filtering, sorting, scrolling, focus and feedback are all
 * here, in loader-independent code, and are therefore covered by ordinary unit
 * tests. The loader bridge supplies a {@link Painter} and materialises
 * {@link UiElement}s as real Minecraft widgets, which is what gives every version
 * and every loader the same interface, the same focus order and the same narration.
 *
 * <p>Interaction model:
 * <ul>
 *   <li>Tab and Shift+Tab walk the controls in visual order;</li>
 *   <li>arrow keys move inside the home and history lists and scroll them into view;</li>
 *   <li>Enter or Space activates the focused control;</li>
 *   <li>{@code /} or Ctrl+F jumps to search, F5 refreshes, Escape goes back;</li>
 *   <li>right click toggles a favourite on a list row or a grid card;</li>
 *   <li>destructive actions ask first, unless the confirmation setting is off.</li>
 * </ul>
 */
public final class HomeGuiUi {

    /** The four sections of the interface. */
    public enum Tab {
        HOMES("tab.homes", Icons.HOME),
        HISTORY("tab.history", Icons.CLOCK),
        STATS("tab.stats", Icons.CHART),
        SETTINGS("tab.settings", Icons.GEAR);

        public final String langKey;
        public final String icon;

        Tab(String langKey, String icon) {
            this.langKey = langKey;
            this.icon = icon;
        }

        public Tab next() {
            Tab[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    /** A short lived confirmation shown in the corner of the panel. */
    private static final class Toast {
        final String message;
        final long expiresAt;

        Toast(String message, long expiresAt) {
            this.message = message;
            this.expiresAt = expiresAt;
        }
    }

    private static final long TOAST_MILLIS = 2_600L;
    private static final long PRESSED_MILLIS = 110L;
    private static final long SPINNER_STEP_MILLIS = 420L;
    private static final long CARET_BLINK_MILLIS = 500L;
    private static final int MAX_SEARCH = 64;
    private static final int MAX_TOASTS = 3;

    private final ConfigStore config;
    private final HomesController homes;
    private final UiHost host;
    private final Localization text;
    private final UiSurface surface = new UiSurface();

    private Tab tab = Tab.HOMES;
    private int width;
    private int height;

    // panel geometry, recomputed on every layout so the interface follows the window
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int pad;
    private int gap;
    private int controlH;
    private int rowH;
    private int cardH;
    private int contentTop;
    private int contentBottom;

    // homes tab state
    private final List<Home> visible = new ArrayList<>();
    private String search = "";
    private int caret;
    private boolean favouritesOnly;
    private int scroll;
    private int visibleRows = 1;
    private int listTop;
    private int listBottom;
    private int lastHoveredIndex = -1;

    // history tab state
    private int historyScroll;
    private int historyVisibleRows = 1;

    // modal state
    private String pendingAction;
    private String pendingMessage;

    private final List<Toast> toasts = new ArrayList<>();
    private String pressedId;
    private long pressedUntil;
    private long lastStateChange;
    private RequestState lastState = RequestState.IDLE;

    public HomeGuiUi(ConfigStore config, HomesController homes, UiHost host, Localization text) {
        this.config = config;
        this.homes = homes;
        this.host = host;
        this.text = text;
    }

    public UiSurface surface() { return surface; }

    public Tab tab() { return tab; }

    public Localization text() { return text; }

    public String searchText() { return search; }

    public int scroll() { return scroll; }

    public List<Home> visibleHomes() { return visible; }

    public boolean isFavouritesOnly() { return favouritesOnly; }

    public boolean isDialogOpen() { return pendingAction != null; }

    /** How many rows or cards fit in the list area at the current size. */
    public int visibleRows() { return tab == Tab.HISTORY ? historyVisibleRows : visibleRows; }

    /** The full filtered and sorted list, not just the rows currently on screen. */
    public int filteredCount() { return visible.size(); }

    public void selectTab(Tab value) {
        tab = value;
        host.invalidate();
    }

    // ------------------------------------------------------------- geometry

    /** Called when the window is resized. */
    public void resize(int newWidth, int newHeight) {
        width = Math.max(120, newWidth);
        height = Math.max(120, newHeight);
        host.invalidate();
    }

    private void computeGeometry() {
        Preferences preferences = config.preferences();
        boolean compact = preferences.density() == Preferences.Density.COMPACT;
        panelW = clamp(240, width - 20, compact ? 360 : 400);
        panelH = clamp(170, height - 20, compact ? 250 : 288);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        pad = compact ? 7 : 10;
        gap = compact ? 4 : 6;
        controlH = compact ? 16 : 20;
        rowH = preferences.rowHeight();
        cardH = preferences.cardHeight();
        contentTop = panelY + pad + controlH + gap + controlH + gap;
        contentBottom = panelY + panelH - pad - controlH - gap;
        listTop = contentTop + controlH + gap;
        listBottom = contentBottom;
    }

    private static int clamp(int minimum, int value, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // --------------------------------------------------------------- layout

    /** Rebuilds the element set for the current size, tab and data. */
    public void layout() {
        computeGeometry();
        String focused = surface.focusedId();
        surface.begin();

        if (pendingAction == null) {
            int tabsY = panelY + pad + controlH + gap;
            int tabW = (panelW - pad * 2 - gap * 3) / 4;
            int index = 0;
            for (Tab value : Tab.values()) {
                UiElement element = surface.add("tab." + value.name(), UiElement.Role.TAB,
                        panelX + pad + index * (tabW + gap), tabsY, tabW, controlH);
                element.label = text.get(value.langKey);
                element.icon = value.icon;
                element.checked = value == tab;
                index++;
            }

            switch (tab) {
                case HOMES:
                    layoutHomes();
                    break;
                case HISTORY:
                    layoutHistory();
                    break;
                case STATS:
                    layoutStats();
                    break;
                case SETTINGS:
                default:
                    layoutSettings();
                    break;
            }
        } else {
            layoutDialog();
        }

        surface.retainFocus(focused);
        if (surface.focused() == null) {
            UiElement first = surface.firstFocusable();
            if (first != null) surface.setFocusedId(first.id);
        }
    }

    private void layoutHomes() {
        int toolbarY = contentTop;
        int searchW = panelW - pad * 2 - controlH - gap;

        UiElement field = surface.add("search", UiElement.Role.FIELD,
                panelX + pad, toolbarY, searchW, controlH);
        field.label = text.get("homegui.hint.search");
        field.value = search;

        UiElement filter = surface.add("favfilter", UiElement.Role.TOGGLE,
                panelX + pad + searchW + gap, toolbarY, controlH, controlH);
        filter.label = text.get("homegui.filter.favourites");
        filter.icon = Icons.STAR;
        filter.checked = favouritesOnly;

        applyFilter();

        int rowStep = rowH + gap;
        boolean grid = config.preferences().viewMode() == Preferences.ViewMode.GRID;
        int columns = config.preferences().gridColumns();
        int listW = panelW - pad * 2;

        if (grid) {
            int cardW = (listW - gap * (columns - 1)) / Math.max(1, columns);
            int rows = Math.max(1, (listBottom - listTop) / (cardH + gap));
            visibleRows = Math.max(1, rows * columns);
            clampScroll(visible.size(), visibleRows);
            int end = Math.min(visible.size(), scroll + visibleRows);
            for (int i = scroll; i < end; i++) {
                int local = i - scroll;
                int x = panelX + pad + (local % columns) * (cardW + gap);
                int y = listTop + (local / columns) * (cardH + gap);
                addHomeElement("home." + i, UiElement.Role.CARD, x, y, cardW, cardH, i, false);
            }
        } else {
            visibleRows = Math.max(1, (listBottom - listTop) / rowStep);
            clampScroll(visible.size(), visibleRows);
            int end = Math.min(visible.size(), scroll + visibleRows);
            int starW = controlH;
            int nameW = listW - starW - gap;
            for (int i = scroll; i < end; i++) {
                int y = listTop + (i - scroll) * rowStep;
                addHomeElement("home." + i, UiElement.Role.ITEM, panelX + pad, y, nameW, rowH, i, true);
                UiElement star = surface.add("fav." + i, UiElement.Role.TOGGLE,
                        panelX + pad + nameW + gap, y, starW, rowH);
                Home home = visible.get(i);
                star.label = text.get("homegui.action.favourite");
                star.hint = home.name();
                star.icon = Icons.STAR;
                star.checked = config.isFavorite(home.name());
                star.payload = home.name();
                star.index = i;
            }
        }

        int footerY = panelY + panelH - pad - controlH;
        int footerW = panelW - pad * 2;
        int buttonW = (footerW - gap * 3) / 4;

        SortMode sortMode = config.preferences().sortMode();
        UiElement sort = surface.add("sort", UiElement.Role.BUTTON, panelX + pad, footerY, buttonW, controlH);
        sort.label = text.get(sortMode.langKey);
        sort.value = text.get("homegui.settings.sort");
        sort.icon = Icons.SORT;

        UiElement view = surface.add("view", UiElement.Role.BUTTON,
                panelX + pad + buttonW + gap, footerY, buttonW, controlH);
        boolean gridNow = config.preferences().viewMode() == Preferences.ViewMode.GRID;
        view.label = text.get(gridNow ? "homegui.view.grid" : "homegui.view.list");
        view.value = text.get("homegui.settings.view");
        view.icon = gridNow ? Icons.GRID : Icons.LIST;

        UiElement refresh = surface.add("refresh", UiElement.Role.BUTTON,
                panelX + pad + (buttonW + gap) * 2, footerY, buttonW, controlH);
        refresh.label = text.get("homegui.button.refresh");
        refresh.icon = Icons.REFRESH;
        refresh.enabled = homes.request().state() != RequestState.LOADING;

        UiElement close = surface.add("close", UiElement.Role.BUTTON,
                panelX + pad + (buttonW + gap) * 3, footerY, buttonW, controlH);
        close.label = text.get("homegui.button.close");
        close.icon = Icons.CLOSE;
    }

    private void addHomeElement(String id, UiElement.Role role, int x, int y, int w, int h,
                                int index, boolean listRow) {
        Home home = visible.get(index);
        UiElement element = surface.add(id, role, x, y, w, h);
        element.label = home.name();
        element.payload = home.name();
        element.icon = listRow ? Icons.HOME : "";
        element.index = index;
        element.checked = config.isFavorite(home.name());
        element.hint = text.get("homegui.action.teleport");

        StringBuilder value = new StringBuilder();
        int uses = config.useCount(home.name());
        if (config.preferences().showUseCounts() && uses > 0) {
            value.append(uses).append(uses == 1
                    ? " " + text.get("homegui.stats.visit")
                    : " " + text.get("homegui.stats.visits"));
        }
        Home.Coordinates coordinates = home.hasCoordinates()
                ? home.coordinates().orElse(null) : config.coordinates(home.name());
        if (coordinates != null && config.preferences().showCoordinates()) {
            if (value.length() > 0) value.append("   ");
            value.append(coordinates.shortText());
        }
        element.value = value.toString();
    }

    private void layoutHistory() {
        List<ConfigStore.HistoryEntry> history = config.history();
        int rowStep = rowH + gap;
        int listW = panelW - pad * 2;
        int top = contentTop;
        int bottom = contentBottom;
        historyVisibleRows = Math.max(1, (bottom - top) / rowStep);
        clampScrollGeneric(history.size(), historyVisibleRows, true);

        int end = Math.min(history.size(), historyScroll + historyVisibleRows);
        for (int i = historyScroll; i < end; i++) {
            ConfigStore.HistoryEntry entry = history.get(i);
            int y = top + (i - historyScroll) * rowStep;
            UiElement element = surface.add("history." + i, UiElement.Role.ITEM,
                    panelX + pad, y, listW, rowH);
            element.label = entry.homeName;
            element.payload = entry.homeName;
            element.icon = Icons.CLOCK;
            element.index = i;
            element.value = relativeTime(entry.timestamp);
            element.hint = text.get("homegui.action.teleport");
        }

        int footerY = panelY + panelH - pad - controlH;
        int footerW = panelW - pad * 2;
        int buttonW = (footerW - gap) / 2;

        UiElement clear = surface.add("history.clear", UiElement.Role.BUTTON,
                panelX + pad, footerY, buttonW, controlH);
        clear.label = text.get("homegui.button.clear");
        clear.icon = Icons.CLOSE;
        clear.destructive = true;
        clear.enabled = !history.isEmpty();

        UiElement back = surface.add("close", UiElement.Role.BUTTON,
                panelX + pad + buttonW + gap, footerY, buttonW, controlH);
        back.label = text.get("homegui.button.close");
        back.icon = Icons.CLOSE;
    }

    private void layoutStats() {
        int footerY = panelY + panelH - pad - controlH;
        int buttonW = panelW - pad * 2;
        UiElement back = surface.add("close", UiElement.Role.BUTTON,
                panelX + pad, footerY, buttonW, controlH);
        back.label = text.get("homegui.button.close");
        back.icon = Icons.CLOSE;
    }

    private void layoutSettings() {
        Preferences preferences = config.preferences();
        int rowStep = controlH + gap;
        int rowW = panelW - pad * 2;
        int half = (rowW - gap) / 2;
        int y = contentTop;
        int maxRows = Math.max(1, (contentBottom - contentTop) / rowStep);
        int row = 0;

        if (row < maxRows) addSettingRow("set.language", y(row, rowStep), rowW,
                text.get("homegui.settings.language"),
                text.get("homegui.language.name", Localization.displayName(preferences.language())),
                Icons.GEAR);
        row++;

        if (row < maxRows) addSettingRow("set.sort", y(row, rowStep), rowW,
                text.get("homegui.settings.sort"),
                text.get(preferences.sortMode().langKey), Icons.SORT);
        row++;

        if (row < maxRows) addSettingRow("set.view", y(row, rowStep), rowW,
                text.get("homegui.settings.view"),
                text.get(preferences.viewMode().langKey),
                preferences.viewMode() == Preferences.ViewMode.GRID ? Icons.GRID : Icons.LIST);
        row++;

        if (row < maxRows) addSettingRow("set.density", y(row, rowStep), rowW,
                text.get("homegui.settings.density"),
                text.get(preferences.density().langKey), Icons.LIST);
        row++;

        if (row < maxRows) addSettingRow("set.theme", y(row, rowStep), rowW,
                text.get("homegui.settings.theme"),
                text.get(Theme.paletteKey(preferences.themeIndex())), Icons.CHECK);
        row++;

        if (row < maxRows) addToggleRow("set.transparent", y(row, rowStep), rowW,
                text.get("homegui.settings.transparent"), preferences.transparentMenu());
        row++;

        if (row < maxRows) addToggleRow("set.coords", y(row, rowStep), rowW,
                text.get("homegui.settings.coordinates"), preferences.showCoordinates());
        row++;

        if (row < maxRows) addToggleRow("set.uses", y(row, rowStep), rowW,
                text.get("homegui.settings.usecounts"), preferences.showUseCounts());
        row++;

        if (row < maxRows) addToggleRow("set.animations", y(row, rowStep), rowW,
                text.get("homegui.settings.animations"), preferences.animationsEnabled());
        row++;

        if (row < maxRows) addToggleRow("set.confirm", y(row, rowStep), rowW,
                text.get("homegui.settings.confirm"), preferences.confirmDestructive());
        row++;

        if (row < maxRows) {
            int columnsY = y(row, rowStep);
            UiElement label = surface.add("set.cols.label", UiElement.Role.TOGGLE,
                    panelX + pad, columnsY, half, controlH);
            label.label = text.get("homegui.settings.columns");
            label.value = String.valueOf(preferences.gridColumns());
            label.icon = Icons.GRID;
            label.hint = text.get("homegui.settings.columns.hint");

            UiElement minus = surface.add("set.cols.minus", UiElement.Role.BUTTON,
                    panelX + pad + half + gap, columnsY, half / 2 - gap / 2, controlH);
            minus.label = text.get("homegui.button.less");
            minus.value = "-";
            minus.enabled = preferences.gridColumns() > Preferences.MIN_GRID_COLUMNS;

            UiElement plus = surface.add("set.cols.plus", UiElement.Role.BUTTON,
                    panelX + pad + half + gap + half / 2 + gap / 2, columnsY,
                    half - half / 2 - gap / 2, controlH);
            plus.label = text.get("homegui.button.more");
            plus.value = "+";
            plus.enabled = preferences.gridColumns() < Preferences.MAX_GRID_COLUMNS;
        }
        row++;

        if (row < maxRows) {
            int actionsY = y(row, rowStep);
            UiElement export = surface.add("set.export", UiElement.Role.BUTTON,
                    panelX + pad, actionsY, half, controlH);
            export.label = text.get("homegui.button.export");
            export.icon = Icons.EXPORT;

            UiElement dataImport = surface.add("set.import", UiElement.Role.BUTTON,
                    panelX + pad + half + gap, actionsY, half, controlH);
            dataImport.label = text.get("homegui.button.import");
            dataImport.icon = Icons.IMPORT;
        }
        row++;

        int footerY = panelY + panelH - pad - controlH;
        UiElement back = surface.add("close", UiElement.Role.BUTTON,
                panelX + pad, footerY, rowW, controlH);
        back.label = text.get("homegui.button.back");
        back.icon = Icons.BACK;
    }

    private int y(int row, int rowStep) {
        return contentTop + row * rowStep;
    }

    private void addSettingRow(String id, int rowY, int rowW, String label, String value, String icon) {
        UiElement element = surface.add(id, UiElement.Role.BUTTON, panelX + pad, rowY, rowW, controlH);
        element.label = label;
        element.value = value;
        element.icon = icon;
    }

    private void addToggleRow(String id, int rowY, int rowW, String label, boolean checked) {
        UiElement element = surface.add(id, UiElement.Role.TOGGLE, panelX + pad, rowY, rowW, controlH);
        element.label = label;
        element.value = text.get(checked ? "homegui.value.on" : "homegui.value.off");
        element.icon = checked ? Icons.CHECK : "";
        element.checked = checked;
    }

    private void layoutDialog() {
        int dialogW = Math.min(panelW - pad * 2, 260);
        int dialogH = controlH * 2 + gap * 3 + 24;
        int dialogX = panelX + (panelW - dialogW) / 2;
        int dialogY = panelY + (panelH - dialogH) / 2;
        int buttonW = (dialogW - gap) / 2;
        int buttonY = dialogY + dialogH - controlH - gap;

        UiElement cancel = surface.add("confirm.cancel", UiElement.Role.BUTTON,
                dialogX + gap, buttonY, buttonW, controlH);
        cancel.label = text.get("homegui.button.cancel");
        cancel.icon = Icons.BACK;
        cancel.primary = true;

        UiElement ok = surface.add("confirm.ok", UiElement.Role.BUTTON,
                dialogX + gap + buttonW + gap, buttonY, buttonW, controlH);
        ok.label = text.get("homegui.button.confirm");
        ok.icon = Icons.CHECK;
        ok.destructive = true;

        // keep geometry for painting
        pendingDialogX = dialogX;
        pendingDialogY = dialogY;
        pendingDialogW = dialogW;
        pendingDialogH = dialogH;
    }

    private int pendingDialogX;
    private int pendingDialogY;
    private int pendingDialogW;
    private int pendingDialogH;

    // --------------------------------------------------------------- data

    private void applyFilter() {
        visible.clear();
        String query = HomeNames.key(search);
        SortMode.HomeIndex index = config.index();
        for (Home home : homes.homes()) {
            if (favouritesOnly && !config.isFavorite(home.name())) continue;
            if (!query.isEmpty() && !HomeNames.key(home.name()).contains(query)) continue;
            visible.add(home);
        }
        config.preferences().sortMode().apply(visible, index);
    }

    private void clampScroll(int total, int rows) {
        scroll = clamp(0, scroll, Math.max(0, total - rows));
    }

    private void clampScrollGeneric(int total, int rows, boolean historyList) {
        if (historyList) historyScroll = clamp(0, historyScroll, Math.max(0, total - rows));
    }

    /** Scrolls so that a list index is inside the visible window. */
    public void ensureVisible(int index) {
        if (index < scroll) {
            scroll = Math.max(0, index);
        } else if (index >= scroll + visibleRows) {
            scroll = index - visibleRows + 1;
        }
        scroll = Math.max(0, scroll);
    }

    private void ensureHistoryVisible(int index) {
        if (index < historyScroll) {
            historyScroll = Math.max(0, index);
        } else if (index >= historyScroll + historyVisibleRows) {
            historyScroll = index - historyVisibleRows + 1;
        }
        historyScroll = Math.max(0, historyScroll);
    }

    private String relativeTime(long timestamp) {
        long seconds = Math.max(0, (host.now() - timestamp) / 1000L);
        if (seconds < 60) return text.get("homegui.time.seconds", seconds);
        if (seconds < 3_600) return text.get("homegui.time.minutes", seconds / 60);
        if (seconds < 86_400) return text.get("homegui.time.hours", seconds / 3_600);
        return text.get("homegui.time.days", seconds / 86_400);
    }

    // -------------------------------------------------------------- tick

    /** Called once per client tick. */
    public void tick() {
        RequestState state = homes.request().state();
        if (state != lastState) {
            lastState = state;
            lastStateChange = host.now();
            host.invalidate();
        }
        long now = host.now();
        boolean expired = toasts.removeIf(toast -> now >= toast.expiresAt);
        if (expired) host.invalidate();
    }

    // ------------------------------------------------------------- input

    /**
     * Called when a control is activated by click, Enter or Space. The layout is
     * rebuilt before returning so the model is never observed half way through a
     * change, whatever the caller does next.
     */
    public boolean activate(String id) {
        boolean handled = doActivate(id);
        if (handled) layout();
        return handled;
    }

    private boolean doActivate(String id) {
        if (id == null) return false;
        if (id.startsWith("tab.")) {
            for (Tab value : Tab.values()) {
                if (id.equals("tab." + value.name())) {
                    tab = value;
                    host.invalidate();
                    host.announce(text.get(value.langKey));
                    return true;
                }
            }
            return false;
        }

        switch (id) {
            case "search":
                return false;
            case "favfilter":
                favouritesOnly = !favouritesOnly;
                scroll = 0;
                host.invalidate();
                host.announce(favouritesOnly
                        ? text.get("homegui.filter.favourites") : text.get("homegui.filter.all"));
                return true;
            case "sort":
                config.preferences().setSortMode(config.preferences().sortMode().next());
                config.savePreferences();
                host.invalidate();
                host.announce(text.get(config.preferences().sortMode().langKey));
                return true;
            case "view":
                config.preferences().setViewMode(config.preferences().viewMode().next());
                config.savePreferences();
                scroll = 0;
                host.invalidate();
                host.announce(text.get(config.preferences().viewMode().langKey));
                return true;
            case "refresh":
                scroll = 0;
                host.refresh();
                return true;
            case "close":
                host.close();
                return true;
            case "history.clear":
                requestConfirm("clearHistory", text.get("homegui.confirm.clear_history"));
                return true;
            case "set.language": {
                String code = text.cycle();
                config.preferences().setLanguage(code);
                config.savePreferences();
                host.invalidate();
                host.announce(Localization.displayName(text.requestedLanguage()));
                return true;
            }
            case "set.sort":
                config.preferences().setSortMode(config.preferences().sortMode().next());
                config.savePreferences();
                host.invalidate();
                return true;
            case "set.view":
                config.preferences().setViewMode(config.preferences().viewMode().next());
                config.savePreferences();
                scroll = 0;
                host.invalidate();
                return true;
            case "set.density":
                config.preferences().setDensity(config.preferences().density().next());
                config.savePreferences();
                host.invalidate();
                return true;
            case "set.theme":
                config.preferences().cycleTheme();
                config.savePreferences();
                host.invalidate();
                host.announce(text.get(Theme.paletteKey(config.preferences().themeIndex())));
                return true;
            case "set.transparent":
                togglePreference(true);
                return true;
            case "set.coords":
                togglePreference(false);
                return true;
            case "set.uses":
                config.preferences().setShowUseCounts(!config.preferences().showUseCounts());
                config.savePreferences();
                host.invalidate();
                return true;
            case "set.animations":
                config.preferences().setAnimationsEnabled(!config.preferences().animationsEnabled());
                config.savePreferences();
                host.invalidate();
                return true;
            case "set.confirm":
                config.preferences().setConfirmDestructive(!config.preferences().confirmDestructive());
                config.savePreferences();
                host.invalidate();
                return true;
            case "set.cols.minus":
                config.preferences().setGridColumns(config.preferences().gridColumns() - 1);
                config.savePreferences();
                scroll = 0;
                host.invalidate();
                return true;
            case "set.cols.plus":
                config.preferences().setGridColumns(config.preferences().gridColumns() + 1);
                config.savePreferences();
                scroll = 0;
                host.invalidate();
                return true;
            case "set.export":
                notify(config.exportData()
                        ? text.get("homegui.message.exported")
                        : text.get("homegui.message.export_failed"));
                return true;
            case "set.import": {
                ConfigStore.ImportResult result = config.importData();
                if (result.fileMissing) notify(text.get("homegui.message.import_missing"));
                else if (!result.isSuccess()) notify(text.get("homegui.message.import_failed"));
                else notify(text.get("homegui.message.imported", result.imported));
                host.invalidate();
                return true;
            }
            case "set.cols.label":
                config.preferences().setGridColumns(config.preferences().gridColumns()
                        >= Preferences.MAX_GRID_COLUMNS
                        ? Preferences.MIN_GRID_COLUMNS
                        : config.preferences().gridColumns() + 1);
                config.savePreferences();
                host.invalidate();
                return true;
            case "confirm.cancel":
                pendingAction = null;
                pendingMessage = null;
                host.invalidate();
                return true;
            case "confirm.ok":
                runConfirmedAction();
                return true;
            default:
                break;
        }

        UiElement element = surface.byId(id);
        if (element == null) return false;
        if (id.startsWith("fav.")) {
            toggleFavourite(element.payload);
            return true;
        }
        if (id.startsWith("home.") || id.startsWith("history.")) {
            if (homes.teleport(element.payload)) {
                notify(text.get("homegui.message.teleporting", element.payload));
                host.close();
            } else {
                notify(text.get("homegui.message.rejected"));
            }
            return true;
        }
        return false;
    }

    private void togglePreference(boolean transparent) {
        if (transparent) {
            config.preferences().setTransparentMenu(!config.preferences().transparentMenu());
        } else {
            config.preferences().setShowCoordinates(!config.preferences().showCoordinates());
        }
        config.savePreferences();
        host.invalidate();
    }

    private void toggleFavourite(String home) {
        boolean nowFavourite = config.toggleFavorite(home);
        host.invalidate();
        host.announce(nowFavourite
                ? text.get("homegui.message.favourite_added", home)
                : text.get("homegui.message.favourite_removed", home));
    }

    private void requestConfirm(String action, String message) {
        if (!config.preferences().confirmDestructive()) {
            pendingAction = action;
            runConfirmedAction();
            return;
        }
        pendingAction = action;
        pendingMessage = message;
        surface.setFocusedId("confirm.cancel");
        host.invalidate();
    }

    private void runConfirmedAction() {
        String action = pendingAction;
        pendingAction = null;
        pendingMessage = null;
        if ("clearHistory".equals(action)) {
            config.clearHistory();
            notify(text.get("homegui.message.history_cleared"));
        }
        host.invalidate();
    }

    /** Keyboard input that the widget layer does not already handle. */
    public boolean keyPressed(int key, boolean control, boolean shift) {
        if (pendingAction != null) {
            if (key == Keys.ESCAPE) {
                activate("confirm.cancel");
                return true;
            }
            if (key == Keys.ENTER) {
                activate("confirm.ok");
                return true;
            }
            return false;
        }
        if (key == Keys.F5) {
            activate("refresh");
            return true;
        }
        if (key == Keys.SLASH || (control && key == Keys.F)) {
            if (tab != Tab.HOMES) {
                tab = Tab.HOMES;
            }
            surface.setFocusedId("search");
            host.invalidate();
            return true;
        }
        if (key == Keys.TAB) {
            UiElement next = surface.moveFocus(shift ? -1 : 1);
            if (next != null) host.announce(next.narration());
            return true;
        }
        boolean inHomes = tab == Tab.HOMES && surface.focusedId() != null
                && (surface.focusedId().startsWith("home.") || surface.focusedId().startsWith("fav."));
        boolean inHistory = tab == Tab.HISTORY && surface.focusedId() != null
                && surface.focusedId().startsWith("history.");
        if (key == Keys.DOWN || key == Keys.UP) {
            int direction = key == Keys.DOWN ? 1 : -1;
            if (inHomes) {
                UiElement next = surface.moveFocusAmong("home.", direction);
                if (next != null) {
                    ensureVisible(next.index);
                    surface.setFocusedId(next.id);
                    host.invalidate();
                    host.announce(next.narration());
                }
                return true;
            }
            if (inHistory) {
                UiElement next = surface.moveFocusAmong("history.", direction);
                if (next != null) {
                    ensureHistoryVisible(next.index);
                    surface.setFocusedId(next.id);
                    host.invalidate();
                    host.announce(next.narration());
                }
                return true;
            }
        }
        if (key == Keys.RIGHT || key == Keys.LEFT) {
            if (config.preferences().viewMode() == Preferences.ViewMode.GRID && inHomes) {
                int direction = key == Keys.RIGHT ? 1 : -1;
                UiElement next = surface.moveFocusAmong("home.", direction);
                if (next != null) {
                    ensureVisible(next.index);
                    surface.setFocusedId(next.id);
                    host.invalidate();
                }
                return true;
            }
            if (surface.focusedId() != null && surface.focusedId().startsWith("tab.")) {
                return activate("tab." + tab.next().name());
            }
        }
        if (key == Keys.ESCAPE) {
            if (tab == Tab.HOMES) {
                host.close();
            } else {
                tab = Tab.HOMES;
                host.invalidate();
            }
            return true;
        }
        return false;
    }

    /** Text input, routed to the search field when it has focus. */
    public boolean charTyped(char character) {
        if (!"search".equals(surface.focusedId())) return false;
        if (character < ' ' || search.length() >= MAX_SEARCH) return false;
        search = search.substring(0, Math.min(caret, search.length()))
                + character
                + search.substring(Math.min(caret, search.length()));
        caret = Math.min(caret + 1, search.length());
        scroll = 0;
        host.invalidate();
        return true;
    }

    /** Search field editing keys. */
    public boolean searchKey(int key, boolean control) {
        if (!"search".equals(surface.focusedId())) return false;
        switch (key) {
            case Keys.BACKSPACE:
                if (caret > 0 && !search.isEmpty()) {
                    search = search.substring(0, caret - 1) + search.substring(caret);
                    caret--;
                }
                break;
            case Keys.DELETE:
                if (caret < search.length()) search = search.substring(0, caret) + search.substring(caret + 1);
                break;
            case Keys.LEFT:
                caret = Math.max(0, caret - 1);
                break;
            case Keys.RIGHT:
                caret = Math.min(search.length(), caret + 1);
                break;
            case Keys.HOME:
                caret = 0;
                break;
            case Keys.END:
                caret = search.length();
                break;
            default:
                if (control && key == Keys.A) {
                    caret = search.length();
                } else if (control && key == Keys.V) {
                    paste();
                } else {
                    return false;
                }
                break;
        }
        scroll = 0;
        host.invalidate();
        return true;
    }

    private void paste() {
        String clipboard = host instanceof ClipboardHost
                ? ((ClipboardHost) host).clipboard() : "";
        if (clipboard == null || clipboard.isEmpty()) return;
        StringBuilder cleaned = new StringBuilder();
        for (char character : clipboard.toCharArray()) {
            if (character >= ' ' && cleaned.length() < MAX_SEARCH) cleaned.append(character);
        }
        search = search.substring(0, caret) + cleaned + search.substring(caret);
        caret = Math.min(caret + cleaned.length(), search.length());
    }

    /** Optional clipboard support, supplied by the loader bridge. */
    public interface ClipboardHost {
        String clipboard();
    }

    /** Mouse wheel over the list areas. */
    public boolean mouseScrolled(double amount) {
        int step = amount > 0 ? -1 : amount < 0 ? 1 : 0;
        if (step == 0) return false;
        if (tab == Tab.HOMES) {
            int max = Math.max(0, visible.size() - visibleRows);
            int next = clamp(0, scroll + step, max);
            if (next == scroll) return false;
            scroll = next;
            host.invalidate();
            return true;
        }
        if (tab == Tab.HISTORY) {
            int max = Math.max(0, config.history().size() - historyVisibleRows);
            int next = clamp(0, historyScroll + step, max);
            if (next == historyScroll) return false;
            historyScroll = next;
            host.invalidate();
            return true;
        }
        return false;
    }

    /** Right click, used to toggle a favourite. */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 1) return false;
        UiElement element = surface.hit(mouseX, mouseY);
        if (element == null) return false;
        if (element.id.startsWith("home.") || element.id.startsWith("fav.")) {
            toggleFavourite(element.payload);
            return true;
        }
        return false;
    }

    /** Keeps the pure Java focus model in sync with the widget that Minecraft focused. */
    public void setFocus(String id) {
        if (id != null && id.equals(surface.focusedId())) return;
        surface.setFocusedId(id);
    }

    public boolean isFocused(String id) {
        return id != null && id.equals(surface.focusedId());
    }

    /** Records a mouse press so the control can render a real pressed state. */
    public void notePress(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        UiElement element = surface.hit(mouseX, mouseY);
        if (element == null) return;
        pressedId = element.id;
        pressedUntil = host.now() + PRESSED_MILLIS;
    }

    public boolean isPressed(String id) {
        return id != null && id.equals(pressedId) && host.now() < pressedUntil;
    }

    // ----------------------------------------------------------- painting

    public Theme.Palette palette() {
        return Theme.palette(config.preferences().themeIndex());
    }

    private boolean transparent() {
        return config.preferences().transparentMenu();
    }

    /** Chrome, drawn before the widgets. */
    public void paintBackground(Painter painter, int screenWidth, int screenHeight) {
        Theme.Palette palette = palette();
        boolean seeThrough = transparent();

        painter.fill(0, 0, screenWidth, screenHeight, Theme.backdrop(palette, seeThrough));

        painter.fill(panelX, panelY, panelW, panelH, Theme.surface(palette.panel, seeThrough));
        painter.fill(panelX, panelY, panelW, 2, Theme.surface(palette.accent, seeThrough));
        painter.border(panelX, panelY, panelW, panelH, Theme.surface(palette.panelEdge, seeThrough));

        String title = text.get("homegui.title.main");
        painter.text(title, panelX + pad, panelY + pad + 2, palette.text, true);

        int count = homes.request().homeCount();
        String chip = text.get("homegui.homes.count", count);
        int chipW = painter.textWidth(chip) + 10;
        int chipX = panelX + panelW - pad - chipW;
        int chipY = panelY + pad;
        painter.fill(chipX, chipY, chipW, controlH, Theme.surface(palette.card, seeThrough));
        painter.border(chipX, chipY, chipW, controlH, Theme.surface(palette.border, seeThrough));
        painter.centeredText(chip, chipX + chipW / 2, chipY + (controlH - painter.lineHeight()) / 2,
                palette.textDim, false);
    }

    /** One control, drawn from inside the matching Minecraft widget. */
    public void paintElement(Painter painter, UiElement element, boolean hovered) {
        boolean focused = isFocused(element.id);
        boolean pressed = isPressed(element.id);
        Theme.Palette palette = palette();
        boolean seeThrough = transparent();
        int x = element.x;
        int y = element.y;
        int w = element.width;
        int h = element.height;

        int background;
        int border;
        int foreground = element.enabled ? palette.text : palette.textFaint;

        switch (element.role) {
            case FIELD:
                background = Theme.surface(palette.input, seeThrough);
                border = focused ? palette.accent : Theme.surface(palette.border, seeThrough);
                painter.fill(x, y, w, h, background);
                painter.border(x, y, w, h, border);
                paintFieldText(painter, element, x, y, w, h, palette, focused);
                return;
            case ITEM:
            case CARD:
                paintHomeCard(painter, element, hovered, focused, pressed, palette, seeThrough);
                return;
            case TAB:
                background = element.checked
                        ? Theme.surface(palette.accentSoft, seeThrough)
                        : hovered ? Theme.surface(palette.cardHover, seeThrough)
                        : Theme.surface(palette.card, seeThrough);
                painter.fill(x, y, w, h, background);
                if (element.checked) painter.fill(x, y + h - 2, w, 2, Theme.surface(palette.accent, seeThrough));
                else painter.border(x, y, w, h, Theme.surface(palette.border, seeThrough));
                foreground = !element.enabled ? palette.textFaint
                        : element.checked ? palette.text : hovered ? palette.text : palette.textDim;
                paintIconAndLabel(painter, element, x, y, w, h, palette, foreground, true);
                paintFocusRing(painter, element, focused, palette, seeThrough);
                return;
            case TOGGLE:
                background = element.checked
                        ? Theme.surface(Theme.mix(palette.card, palette.favourite, 0.25F), seeThrough)
                        : hovered ? Theme.surface(palette.cardHover, seeThrough)
                        : Theme.surface(palette.card, seeThrough);
                painter.fill(x, y, w, h, background);
                painter.border(x, y, w, h, Theme.surface(
                        element.checked ? palette.favourite : palette.border, seeThrough));
                if (!element.icon.isEmpty()) {
                    int scale = Math.max(1, (Math.min(w, h) - 4) / Icons.SIZE);
                    int iconSize = Icons.sizeAt(scale);
                    Icons.paint(painter, element.icon,
                            x + (w - iconSize) / 2, y + (h - iconSize) / 2, scale,
                            element.enabled
                                    ? (element.checked ? palette.favourite : palette.textFaint)
                                    : palette.textFaint);
                }
                paintFocusRing(painter, element, focused, palette, seeThrough);
                return;
            case BUTTON:
            default:
                int accent = element.destructive ? palette.danger : palette.accent;
                background = pressed ? Theme.surface(Theme.mix(palette.card, 0xFF000000, 0.45F), seeThrough)
                        : hovered ? Theme.surface(palette.cardHover, seeThrough)
                        : Theme.surface(palette.card, seeThrough);
                painter.fill(x, y, w, h, background);
                painter.border(x, y, w, h, Theme.surface(
                        hovered || element.primary ? accent : palette.border, seeThrough));
                if (element.primary) painter.fill(x, y, 2, h, Theme.surface(accent, seeThrough));
                foreground = !element.enabled ? palette.textFaint
                        : element.destructive && hovered ? palette.danger : palette.text;
                if (element.value.length() > 2 && element.icon.isEmpty()) {
                    paintSettingRow(painter, element, palette, seeThrough, foreground);
                } else {
                    paintIconAndLabel(painter, element, x, y, w, h, palette, foreground, true);
                }
                paintFocusRing(painter, element, focused, palette, seeThrough);
                return;
        }
    }

    private void paintFocusRing(Painter painter, UiElement element, boolean focused,
                                Theme.Palette palette, boolean seeThrough) {
        if (!focused) return;
        painter.border(element.x - 1, element.y - 1, element.width + 2, element.height + 2,
                Theme.surface(palette.accent, seeThrough));
    }

    private void paintFieldText(Painter painter, UiElement element, int x, int y, int w, int h,
                                Theme.Palette palette, boolean focused) {
        int textY = y + (h - painter.lineHeight()) / 2;
        String value = search;
        if (value.isEmpty()) {
            painter.text(text.get("homegui.hint.search"), x + 5, textY, palette.textFaint, false);
            return;
        }
        painter.text(painter.truncate(value, w - 10), x + 5, textY, palette.text, false);
        boolean showCaret = focused && (!config.preferences().animationsEnabled()
                || (host.now() / CARET_BLINK_MILLIS) % 2 == 0);
        if (showCaret) {
            int caretX = x + 5 + painter.textWidth(painter.truncate(
                    value.substring(0, Math.min(caret, value.length())), w - 10));
            painter.fill(caretX, y + 3, 1, h - 6, palette.accent);
        }
    }

    private void paintSettingRow(Painter painter, UiElement element, Theme.Palette palette,
                                 boolean seeThrough, int foreground) {
        int textY = element.y + (element.height - painter.lineHeight()) / 2;
        String value = painter.truncate(element.value, element.width / 2 - 8);
        int valueW = painter.textWidth(value);
        painter.text(painter.truncate(element.label, element.width - valueW - 16),
                element.x + 6, textY, palette.textDim, false);
        painter.text(value, element.x + element.width - valueW - 6, textY, foreground, false);
    }

    private void paintIconAndLabel(Painter painter, UiElement element, int x, int y, int w, int h,
                                   Theme.Palette palette, int foreground, boolean centered) {
        int textY = y + (h - painter.lineHeight()) / 2;
        if (element.icon.isEmpty()) {
            String label = painter.truncate(element.label, w - 10);
            if (centered) painter.centeredText(label, x + w / 2, textY, foreground, true);
            else painter.text(label, x + 5, textY, foreground, false);
            return;
        }
        int scale = Math.max(1, (h - 6) / Icons.SIZE);
        int iconSize = Icons.sizeAt(scale);
        String label = painter.truncate(element.label, w - iconSize - 12);
        int total = iconSize + 3 + painter.textWidth(label);
        int startX = centered ? x + (w - total) / 2 : x + 5;
        Icons.paint(painter, element.icon, startX, y + (h - iconSize) / 2, scale,
                element.enabled ? palette.textDim : palette.textFaint);
        painter.text(label, startX + iconSize + 3, textY, foreground, true);
    }

    private void paintHomeCard(Painter painter, UiElement element, boolean hovered, boolean focused,
                               boolean pressed, Theme.Palette palette, boolean seeThrough) {
        int x = element.x;
        int y = element.y;
        int w = element.width;
        int h = element.height;
        boolean grid = element.role == UiElement.Role.CARD;

        int background = pressed ? Theme.surface(Theme.mix(palette.card, 0xFF000000, 0.4F), seeThrough)
                : hovered ? Theme.surface(palette.cardHover, seeThrough)
                : Theme.surface(palette.card, seeThrough);
        painter.fill(x, y, w, h, background);
        painter.border(x, y, w, h, Theme.surface(hovered ? palette.accent : palette.border, seeThrough));
        if (element.checked) {
            painter.fill(x, y, grid ? w : 2, grid ? 2 : h, Theme.surface(palette.favourite, seeThrough));
        }
        if (focused) paintFocusRing(painter, element, true, palette, seeThrough);

        int lineH = painter.lineHeight();
        if (grid) {
            int scale = Math.max(1, (lineH) / Icons.SIZE);
            int iconSize = Icons.sizeAt(scale);
            if (element.checked) {
                Icons.paint(painter, Icons.STAR, x + w - iconSize - 3, y + 3, scale, palette.favourite);
            }
            painter.centeredText(painter.truncate(element.label, w - 10),
                    x + w / 2, y + 6, palette.text, true);
            if (!element.value.isEmpty()) {
                painter.centeredText(painter.truncate(element.value, w - 10),
                        x + w / 2, y + h - lineH - 5, palette.textDim, false);
            }
            return;
        }

        int scale = Math.max(1, (h - 8) / Icons.SIZE);
        int iconSize = Icons.sizeAt(scale);
        int textX = x + 6 + iconSize + 4;
        Icons.paint(painter, Icons.HOME, x + 6, y + (h - iconSize) / 2, scale,
                hovered ? palette.accent : palette.textFaint);
        int valueW = element.value.isEmpty() ? 0 : painter.textWidth(element.value) + 8;
        painter.text(painter.truncate(element.label, w - (textX - x) - valueW - 6),
                textX, y + (h - lineH) / 2, palette.text, true);
        if (valueW > 0) {
            painter.text(element.value, x + w - valueW, y + (h - lineH) / 2, palette.textDim, false);
        }
        if (element.checked) {
            Icons.paint(painter, Icons.STAR, x + w - iconSize - 4 - valueW, y + (h - iconSize) / 2,
                    scale, palette.favourite);
        }
    }

    /** Status strip, empty states, dialog and toasts, drawn after the widgets. */
    public void paintOverlay(Painter painter, int screenWidth, int screenHeight) {
        Theme.Palette palette = palette();
        boolean seeThrough = transparent();
        int lineH = painter.lineHeight();

        if (pendingAction != null) {
            painter.fill(0, 0, screenWidth, screenHeight, 0x88000000);
            painter.fill(pendingDialogX, pendingDialogY, pendingDialogW, pendingDialogH,
                    Theme.surface(palette.panel, seeThrough));
            painter.border(pendingDialogX, pendingDialogY, pendingDialogW, pendingDialogH,
                    Theme.surface(palette.danger, seeThrough));
            int iconSize = Icons.sizeAt(2);
            Icons.paint(painter, Icons.WARN, pendingDialogX + (pendingDialogW - iconSize) / 2,
                    pendingDialogY + gap, 2, palette.warning);
            List<String> lines = wrap(painter, pendingMessage == null ? "" : pendingMessage,
                    pendingDialogW - pad * 2);
            int textY = pendingDialogY + gap + iconSize + 4;
            for (String line : lines) {
                painter.centeredText(line, pendingDialogX + pendingDialogW / 2, textY, palette.text, true);
                textY += lineH + 2;
            }
            paintToasts(painter, palette, seeThrough, lineH);
            return;
        }

        if (tab == Tab.HOMES) {
            paintHomesStatus(painter, palette, seeThrough, lineH);
        } else if (tab == Tab.HISTORY) {
            paintHistoryStatus(painter, palette, seeThrough, lineH);
        } else if (tab == Tab.STATS) {
            paintStats(painter, palette, seeThrough, lineH);
        } else {
            paintSettingsFooter(painter, palette, lineH);
        }

        paintToasts(painter, palette, seeThrough, lineH);
    }

    private void paintHomesStatus(Painter painter, Theme.Palette palette, boolean seeThrough, int lineH) {
        RequestState state = homes.request().state();
        int centreY = listTop + Math.max(0, (listBottom - listTop) / 2);

        if (!visible.isEmpty()) {
            paintScrollbar(painter, palette, seeThrough, visible.size(), visibleRows, scroll, listTop, listBottom);
            return;
        }

        if (state == RequestState.LOADING) {
            Icons.paint(painter, Icons.REFRESH, panelX + panelW / 2 - Icons.SIZE, centreY - lineH - 6,
                    1, palette.accent);
            String label = text.get("homegui.state.loading");
            if (config.preferences().animationsEnabled()) {
                int dots = (int) ((host.now() / SPINNER_STEP_MILLIS) % 4);
                StringBuilder suffix = new StringBuilder();
                for (int i = 0; i < dots; i++) suffix.append('.');
                label = label + suffix;
            }
            painter.centeredText(label, panelX + panelW / 2, centreY + 2, palette.textDim, true);
            return;
        }

        String icon = Icons.BOX;
        String title;
        String hint = "";
        switch (state) {
            case TIMEOUT:
                icon = Icons.WARN;
                title = text.get("homegui.state.timeout");
                hint = text.get("homegui.state.timeout.hint");
                break;
            case UNRECOGNISED:
                icon = Icons.WARN;
                title = text.get("homegui.state.unrecognised");
                hint = text.get("homegui.state.unrecognised.hint");
                break;
            case DISCONNECTED:
                icon = Icons.WARN;
                title = text.get("homegui.state.disconnected");
                break;
            case FAILED:
                icon = Icons.WARN;
                title = text.get("homegui.state.failed");
                break;
            case EMPTY:
                title = text.get("homegui.state.no_homes");
                hint = text.get("homegui.state.no_homes.hint");
                break;
            case READY:
            default:
                if (!homes.homes().isEmpty() && favouritesOnly) {
                    icon = Icons.STAR;
                    title = text.get("homegui.state.no_favourites");
                } else {
                    icon = Icons.SEARCH;
                    title = text.get("homegui.state.no_results", search);
                }
                break;
        }

        Icons.paint(painter, icon, panelX + panelW / 2 - Icons.SIZE, centreY - lineH - 8, 1,
                state == RequestState.TIMEOUT || state == RequestState.UNRECOGNISED
                        ? palette.warning : palette.textFaint);
        painter.centeredText(title, panelX + panelW / 2, centreY, palette.textDim, true);
        if (!hint.isEmpty()) {
            painter.centeredText(painter.truncate(hint, panelW - pad * 2),
                    panelX + panelW / 2, centreY + lineH + 4, palette.textFaint, false);
        }
    }

    private void paintHistoryStatus(Painter painter, Theme.Palette palette, boolean seeThrough, int lineH) {
        int total = config.history().size();
        if (total == 0) {
            int centreY = contentTop + Math.max(0, (contentBottom - contentTop) / 2);
            Icons.paint(painter, Icons.BOX, panelX + panelW / 2 - Icons.SIZE, centreY - lineH - 6, 1,
                    palette.textFaint);
            painter.centeredText(text.get("homegui.state.no_history"),
                    panelX + panelW / 2, centreY, palette.textDim, true);
            return;
        }
        paintScrollbar(painter, palette, seeThrough, total, historyVisibleRows, historyScroll,
                contentTop, contentBottom);
    }

    private void paintScrollbar(Painter painter, Theme.Palette palette, boolean seeThrough,
                                int total, int rows, int offset, int top, int bottom) {
        if (total <= rows) return;
        int trackX = panelX + panelW - pad - 3;
        int trackH = bottom - top;
        if (trackH <= 0) return;
        int thumbH = Math.max(12, trackH * rows / total);
        int maxOffset = Math.max(1, total - rows);
        int thumbY = top + (trackH - thumbH) * Math.min(offset, maxOffset) / maxOffset;
        painter.fill(trackX, top, 3, trackH, Theme.surface(palette.input, seeThrough));
        painter.fill(trackX, thumbY, 3, thumbH, Theme.surface(palette.accent, seeThrough));
    }

    private void paintStats(Painter painter, Theme.Palette palette, boolean seeThrough, int lineH) {
        Map<String, Integer> counts = config.useCounts();
        int top = contentTop;
        int cardW = (panelW - pad * 2 - gap * 2) / 3;
        int cardH2 = controlH * 2 + 4;

        drawStatCard(painter, panelX + pad, top, cardW, cardH2,
                String.valueOf(homes.request().homeCount()), text.get("homegui.stats.total_homes"),
                palette.accent, palette, seeThrough);
        drawStatCard(painter, panelX + pad + cardW + gap, top, cardW, cardH2,
                String.valueOf(config.favoriteKeys().size()), text.get("homegui.stats.favorites"),
                palette.favourite, palette, seeThrough);
        drawStatCard(painter, panelX + pad + (cardW + gap) * 2, top, cardW, cardH2,
                String.valueOf(config.totalTeleports()), text.get("homegui.stats.total_tp"),
                palette.success, palette, seeThrough);

        int barsY = top + cardH2 + gap * 2;
        painter.text(text.get("homegui.stats.top_homes"), panelX + pad, barsY, palette.textDim, true);
        barsY += lineH + 4;

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort((left, right) -> {
            int byCount = Integer.compare(right.getValue(), left.getValue());
            return byCount != 0 ? byCount : left.getKey().compareToIgnoreCase(right.getKey());
        });

        if (ranked.isEmpty()) {
            Icons.paint(painter, Icons.BOX, panelX + panelW / 2 - Icons.SIZE,
                    barsY + gap, 1, palette.textFaint);
            painter.centeredText(text.get("homegui.stats.no_data"),
                    panelX + panelW / 2, barsY + lineH + gap * 3, palette.textDim, true);
            return;
        }

        int max = Math.max(1, ranked.get(0).getValue());
        int barW = panelW - pad * 2;
        int barH = Math.max(10, rowH - 4);
        int limit = Math.min(5, ranked.size());
        for (int i = 0; i < limit && barsY + barH < contentBottom; i++) {
            Map.Entry<String, Integer> entry = ranked.get(i);
            int fill = Math.max(2, barW * entry.getValue() / max);
            int colour = i == 0 ? palette.favourite : i == 1 ? palette.textDim : palette.accentSoft;
            painter.fill(panelX + pad, barsY, barW, barH, Theme.surface(palette.card, seeThrough));
            painter.fill(panelX + pad, barsY, fill, barH, Theme.surface(colour, seeThrough));
            painter.border(panelX + pad, barsY, barW, barH, Theme.surface(palette.border, seeThrough));
            String label = painter.truncate(entry.getKey(), barW - 46);
            painter.text(label, panelX + pad + 5, barsY + (barH - lineH) / 2, palette.text, true);
            String value = String.valueOf(entry.getValue());
            painter.text(value, panelX + pad + barW - painter.textWidth(value) - 5,
                    barsY + (barH - lineH) / 2, palette.textDim, false);
            barsY += barH + gap;
        }
    }

    private void drawStatCard(Painter painter, int x, int y, int w, int h, String value, String label,
                              int accent, Theme.Palette palette, boolean seeThrough) {
        painter.fill(x, y, w, h, Theme.surface(palette.card, seeThrough));
        painter.fill(x, y, w, 2, Theme.surface(accent, seeThrough));
        painter.border(x, y, w, h, Theme.surface(palette.border, seeThrough));
        painter.centeredText(value, x + w / 2, y + 6, accent, true);
        painter.centeredText(painter.truncate(label, w - 8), x + w / 2, y + h - painter.lineHeight() - 4,
                palette.textDim, false);
    }

    private void paintSettingsFooter(Painter painter, Theme.Palette palette, int lineH) {
        String hint = text.get("homegui.settings.data_hint", config.configPath().getFileName().toString());
        painter.centeredText(painter.truncate(hint, panelW - pad * 2),
                panelX + panelW / 2, contentBottom - lineH, palette.textFaint, false);
    }

    private void paintToasts(Painter painter, Theme.Palette palette, boolean seeThrough, int lineH) {
        if (toasts.isEmpty()) return;
        int index = 0;
        for (Toast toast : toasts) {
            int w = Math.min(panelW - pad * 2, painter.textWidth(toast.message) + 16);
            int h = lineH + 6;
            int x = panelX + panelW - pad - w;
            int y = panelY + panelH + 4 + index * (h + 3);
            if (y + h > height) break;
            painter.fill(x, y, w, h, Theme.surface(palette.panel, seeThrough));
            painter.border(x, y, w, h, Theme.surface(palette.accent, seeThrough));
            painter.text(toast.message, x + 8, y + 3, palette.text, true);
            index++;
        }
    }

    private void notify(String message) {
        toasts.removeIf(toast -> toast.message.equals(message));
        toasts.add(0, new Toast(message, host.now() + TOAST_MILLIS));
        while (toasts.size() > MAX_TOASTS) toasts.remove(toasts.size() - 1);
        host.announce(message);
        host.feedback(message);
        host.invalidate();
    }

    private List<String> wrap(Painter painter, String message, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : message.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (painter.textWidth(candidate) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    /** Narration text for one element, used by the widget's narration output. */
    public String narrationFor(String id) {
        UiElement element = surface.byId(id);
        return element == null ? "" : element.narration();
    }

    /** Key codes used by the interface. Values match LWJGL so the bridge can pass them through. */
    public static final class Keys {
        public static final int ESCAPE = 256;
        public static final int ENTER = 257;
        public static final int TAB = 258;
        public static final int BACKSPACE = 259;
        public static final int DELETE = 261;
        public static final int RIGHT = 262;
        public static final int LEFT = 263;
        public static final int DOWN = 264;
        public static final int UP = 265;
        public static final int HOME = 268;
        public static final int END = 269;
        public static final int F = 70;
        public static final int A = 65;
        public static final int V = 86;
        public static final int SLASH = 47;
        public static final int F5 = 294;

        private Keys() {}
    }

    /** Sorting helper kept here so the visible list order is reproducible in tests. */
    public static Comparator<Home> alphabetical() {
        return (a, b) -> a.name().compareToIgnoreCase(b.name());
    }
}
