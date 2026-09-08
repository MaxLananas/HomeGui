package com.maxlananas.homegui.ui;

import com.maxlananas.homegui.core.ConfigStore;
import com.maxlananas.homegui.core.Home;
import com.maxlananas.homegui.core.HomesController;
import com.maxlananas.homegui.core.Preferences;
import com.maxlananas.homegui.core.RequestState;
import com.maxlananas.homegui.core.SortMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeGuiUiTest {

    private static final class FakeSender implements HomesController.CommandSender {
        final List<String> sent = new ArrayList<>();

        @Override
        public boolean send(String command) {
            sent.add(command);
            return true;
        }
    }

    private final FakeSender sender = new FakeSender();
    private final long[] clock = {1_000_000L};
    private final ConfigStore config = ConfigStore.inMemory();
    private final HomesController homes = new HomesController(sender, () -> clock[0], 6_000L);
    private final RecordingHost host = new RecordingHost();
    private final Localization text = new Localization();
    private HomeGuiUi ui;

    @BeforeEach
    void setUp() {
        config.setCurrentServer("play.example.net");
        text.setLanguage("en");
        ui = new HomeGuiUi(config, homes, host, text);
        ui.resize(400, 300);
    }

    private void receive(String... names) {
        homes.requestHomes();
        homes.onChatLine("Homes: " + String.join(", ", names));
        ui.layout();
    }

    private List<String> elementIds() {
        return ui.surface().elements().stream().map(element -> element.id).collect(Collectors.toList());
    }

    private List<String> visibleNames() {
        return ui.visibleHomes().stream().map(Home::name).collect(Collectors.toList());
    }

    // ------------------------------------------------------------- structure

    @Test
    void theNavigationBarIsAlwaysPresent() {
        receive("base");
        List<String> ids = elementIds();
        assertTrue(ids.contains("tab.HOMES"));
        assertTrue(ids.contains("tab.HISTORY"));
        assertTrue(ids.contains("tab.STATS"));
        assertTrue(ids.contains("tab.SETTINGS"));
        assertTrue(ids.contains("search"));
        assertTrue(ids.contains("refresh"));
        assertTrue(ids.contains("close"));
    }

    @Test
    void theLayoutFollowsTheWindowSize() {
        receive("base");
        UiElement small = ui.surface().byId("close");
        ui.resize(200, 160);
        ui.layout();
        UiElement compact = ui.surface().byId("close");
        assertNotNull(compact);
        assertTrue(compact.width <= small.width, "narrow windows get narrower controls");
    }

    @Test
    void everyControlCarriesAnAccessibleName() {
        receive("base", "farm");
        for (UiElement element : ui.surface().elements()) {
            assertFalse(element.label.isEmpty(), element.id + " has no accessible name");
        }
    }

    @Test
    void focusStartsOnTheFirstControlAndWalksEveryOneOfThem() {
        receive("base", "farm");
        int focusable = (int) ui.surface().elements().stream().filter(UiElement::isFocusable).count();
        assertTrue(focusable > 5);
        for (int i = 0; i < focusable; i++) {
            assertNotNull(ui.surface().focused());
            ui.surface().moveFocus(1);
        }
        assertNotNull(ui.surface().focused(), "focus wraps instead of disappearing");
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchingNarrowsTheList() {
        receive("base", "farm", "nether_mine");
        ui.setFocus("search");
        ui.charTyped('n');
        ui.charTyped('e');
        assertEquals(List.of("nether_mine"), visibleNames());
    }

    @Test
    void backspaceUndoesTheLastCharacter() {
        receive("base", "farm");
        ui.setFocus("search");
        ui.charTyped('b');
        ui.charTyped('x');
        assertTrue(ui.visibleHomes().isEmpty());
        ui.searchKey(HomeGuiUi.Keys.BACKSPACE, false);
        assertEquals(List.of("base"), visibleNames());
    }

    @Test
    void aSearchWithNoMatchIsReportedRatherThanLookingEmpty() {
        receive("base");
        ui.setFocus("search");
        ui.charTyped('z');
        RecordingPainter painter = new RecordingPainter();
        ui.paintBackground(painter, 400, 300);
        ui.paintOverlay(painter, 400, 300);
        assertTrue(painter.painted("No match for"));
    }

    @Test
    void pastingIsBoundedAndStripsControlCharacters() {
        receive("base", "farm");
        host.clipboard = "fa\u0000rm";
        ui.setFocus("search");
        ui.searchKey(HomeGuiUi.Keys.V, true);
        assertEquals("farm", ui.searchText());
    }

    // -------------------------------------------------------------- filtering

    @Test
    void theFavouriteFilterShowsOnlyFavourites() {
        receive("base", "farm");
        config.toggleFavorite("farm");
        ui.layout();
        ui.activate("favfilter");
        assertEquals(List.of("farm"), visibleNames());
        ui.activate("favfilter");
        assertEquals(2, ui.visibleHomes().size());
    }

    @Test
    void changingTheSortOrderReordersTheList() {
        receive("zeta", "alpha", "mid");
        config.preferences().setSortMode(SortMode.ALPHABETICAL);
        ui.layout();
        assertEquals(List.of("alpha", "mid", "zeta"), visibleNames());
        ui.activate("sort");
        assertEquals(SortMode.MOST_USED, config.preferences().sortMode());
    }

    // ------------------------------------------------------------- pagination

    @Test
    void scrollingIsClampedAtBothEnds() {
        String[] names = new String[60];
        for (int i = 0; i < names.length; i++) names[i] = "home" + i;
        receive(names);
        assertTrue(ui.visibleRows() < names.length, "the list must be paginated");

        for (int i = 0; i < 200; i++) ui.mouseScrolled(-1);
        assertEquals(names.length - ui.visibleRows(), ui.scroll());

        for (int i = 0; i < 200; i++) ui.mouseScrolled(1);
        assertEquals(0, ui.scroll());
    }

    @Test
    void arrowKeysScrollTheFocusedRowIntoView() {
        String[] names = new String[60];
        for (int i = 0; i < names.length; i++) names[i] = "home" + i;
        receive(names);
        ui.setFocus("home.0");
        for (int i = 0; i < ui.visibleRows() + 3; i++) {
            ui.keyPressed(HomeGuiUi.Keys.DOWN, false, false);
        }
        assertTrue(ui.scroll() > 0, "the list followed the focus");
    }

    @Test
    void shrinkingTheWindowDoesNotLeaveTheScrollOffsetOutsideTheList() {
        String[] names = new String[60];
        for (int i = 0; i < names.length; i++) names[i] = "home" + i;
        receive(names);
        for (int i = 0; i < 40; i++) ui.mouseScrolled(-1);
        int before = ui.scroll();
        assertTrue(before > 0);
        ui.resize(300, 160);
        ui.layout();
        assertTrue(ui.scroll() <= names.length - ui.visibleRows());
    }

    // ---------------------------------------------------------------- states

    @Test
    void theLoadingStateIsPaintedWithoutAnyHomes() {
        homes.requestHomes();
        ui.layout();
        RecordingPainter painter = new RecordingPainter();
        ui.paintBackground(painter, 400, 300);
        ui.paintOverlay(painter, 400, 300);
        assertTrue(painter.painted("Requesting homes"));
    }

    @Test
    void aTimeoutIsExplainedAndOffersAHint() {
        homes.requestHomes();
        clock[0] += 7_000;
        homes.tick();
        ui.tick();
        ui.layout();
        RecordingPainter painter = new RecordingPainter();
        ui.paintOverlay(painter, 400, 300);
        assertTrue(painter.painted("No response"));
        assertTrue(painter.painted("/homes"));
    }

    @Test
    void anUnreadableReplyIsReportedAsSuch() {
        homes.requestHomes();
        homes.onChatLine("<Steve> anyone about?");
        clock[0] += 7_000;
        homes.tick();
        ui.layout();
        RecordingPainter painter = new RecordingPainter();
        ui.paintOverlay(painter, 400, 300);
        assertTrue(painter.painted("Unrecognised format"));
    }

    @Test
    void anEmptyServerShowsTheSetupHint() {
        homes.requestHomes();
        homes.onChatLine("You have no homes!");
        ui.layout();
        RecordingPainter painter = new RecordingPainter();
        ui.paintOverlay(painter, 400, 300);
        assertTrue(painter.painted("No homes yet"));
        assertTrue(painter.painted("/sethome"));
    }

    // -------------------------------------------------------------- actions

    @Test
    void clickingAHomeSendsTheCommandAndCloses() {
        receive("base", "farm");
        assertTrue(ui.activate("home.0"));
        assertEquals(List.of("home base"), sender.sent);
        assertEquals(1, host.closes);
    }

    @Test
    void rightClickingAHomeTogglesItsFavourite() {
        receive("base", "farm");
        UiElement card = ui.surface().byId("home.1");
        assertTrue(ui.mouseClicked(card.x + 2, card.y + 2, 1));
        assertTrue(config.isFavorite("farm"));
    }

    @Test
    void theStarButtonTogglesTheSameFavourite() {
        receive("base");
        ui.activate("fav.0");
        assertTrue(config.isFavorite("base"));
        ui.activate("fav.0");
        assertFalse(config.isFavorite("base"));
    }

    @Test
    void refreshSendsTheListCommandAgain() {
        receive("base");
        ui.activate("refresh");
        assertEquals(2, sender.sent.size());
        assertEquals(RequestState.LOADING, homes.request().state());
    }

    @Test
    void theViewSettingSwitchesBetweenListAndGrid() {
        receive("base", "farm", "mine");
        assertEquals(Preferences.ViewMode.LIST, config.preferences().viewMode());
        ui.activate("view");
        assertEquals(Preferences.ViewMode.GRID, config.preferences().viewMode());
        assertTrue(ui.surface().elements().stream()
                .anyMatch(element -> element.role == UiElement.Role.CARD));
        ui.activate("view");
        assertEquals(Preferences.ViewMode.LIST, config.preferences().viewMode());
    }

    @Test
    void theGridColumnSettingChangesTheLayout() {
        receive("base", "farm", "mine");
        config.preferences().setViewMode(Preferences.ViewMode.GRID);
        config.preferences().setGridColumns(2);
        ui.layout();
        assertNotNull(ui.surface().byId("home.1"));
        ui.activate("set.cols.plus");
        assertEquals(3, config.preferences().gridColumns());
    }

    // --------------------------------------------------------- confirmation

    @Test
    void clearingHistoryAsksFirst() {
        receive("base");
        config.recordTeleport("base", null, System.currentTimeMillis());
        ui.selectTab(HomeGuiUi.Tab.HISTORY);
        ui.layout();
        ui.activate("history.clear");
        assertTrue(ui.isDialogOpen());

        ui.layout();
        ui.activate("confirm.cancel");
        assertFalse(ui.isDialogOpen());
        assertEquals(1, config.history().size());
    }

    @Test
    void confirmingActuallyClears() {
        config.recordTeleport("base", null, System.currentTimeMillis());
        ui.selectTab(HomeGuiUi.Tab.HISTORY);
        ui.layout();
        ui.activate("history.clear");
        ui.layout();
        ui.activate("confirm.ok");
        assertTrue(config.history().isEmpty());
    }

    @Test
    void confirmationCanBeTurnedOff() {
        config.preferences().setConfirmDestructive(false);
        config.recordTeleport("base", null, System.currentTimeMillis());
        ui.selectTab(HomeGuiUi.Tab.HISTORY);
        ui.layout();
        ui.activate("history.clear");
        assertFalse(ui.isDialogOpen());
        assertTrue(config.history().isEmpty());
    }

    // ------------------------------------------------------------- feedback

    @Test
    void exportAndImportReportWhatHappened() {
        receive("base");
        config.toggleFavorite("base");
        ui.selectTab(HomeGuiUi.Tab.SETTINGS);
        ui.layout();

        ui.activate("set.import");
        assertTrue(host.announced.stream().anyMatch(message -> message.contains("No export file")),
                "a missing export file is reported instead of failing silently");
    }

    @Test
    void everySettingChangesSomething() {
        receive("base");
        ui.selectTab(HomeGuiUi.Tab.SETTINGS);
        ui.layout();

        ui.activate("set.language");
        assertEquals("fr", config.preferences().language());
        ui.activate("set.density");
        assertEquals(Preferences.Density.COMPACT, config.preferences().density());
        ui.activate("set.theme");
        assertEquals(1, config.preferences().themeIndex());
        ui.activate("set.transparent");
        assertTrue(config.preferences().transparentMenu());
        ui.activate("set.coords");
        assertFalse(config.preferences().showCoordinates());
        ui.activate("set.uses");
        assertFalse(config.preferences().showUseCounts());
        ui.activate("set.animations");
        assertFalse(config.preferences().animationsEnabled());
        ui.activate("set.confirm");
        assertFalse(config.preferences().confirmDestructive());
    }

    // -------------------------------------------------------------- painting

    @Test
    void paintingEveryTabProducesOutputWithoutThrowing() {
        receive("base", "farm");
        config.recordTeleport("base", null, System.currentTimeMillis());
        RecordingPainter painter = new RecordingPainter();
        for (HomeGuiUi.Tab tab : HomeGuiUi.Tab.values()) {
            ui.selectTab(tab);
            ui.layout();
            painter.operations.clear();
            ui.paintBackground(painter, 400, 300);
            for (UiElement element : ui.surface().elements()) {
                ui.paintElement(painter, element, element.id.equals("home.0"));
            }
            ui.paintOverlay(painter, 400, 300);
            assertTrue(painter.fills > 0, tab + " painted nothing");
        }
    }

    @Test
    void aTinyWindowStillProducesAUsableLayout() {
        receive("base");
        ui.resize(140, 120);
        ui.layout();
        assertNotNull(ui.surface().byId("close"));
        RecordingPainter painter = new RecordingPainter();
        ui.paintBackground(painter, 140, 120);
        ui.paintOverlay(painter, 140, 120);
        assertTrue(painter.fills > 0);
    }

    @Test
    void keyboardShortcutsReachSearchAndRefresh() {
        receive("base");
        ui.setFocus("close");
        assertTrue(ui.keyPressed(HomeGuiUi.Keys.SLASH, false, false));
        assertEquals("search", ui.surface().focusedId());
        assertTrue(ui.keyPressed(HomeGuiUi.Keys.F5, false, false));
        assertEquals(1, host.refreshes, "F5 asks the loader bridge to request the list again");
    }

    @Test
    void escapeLeavesASubTabBeforeClosing() {
        receive("base");
        ui.selectTab(HomeGuiUi.Tab.SETTINGS);
        ui.keyPressed(HomeGuiUi.Keys.ESCAPE, false, false);
        assertEquals(HomeGuiUi.Tab.HOMES, ui.tab());
        assertEquals(0, host.closes);
        ui.keyPressed(HomeGuiUi.Keys.ESCAPE, false, false);
        assertEquals(1, host.closes);
    }
}
