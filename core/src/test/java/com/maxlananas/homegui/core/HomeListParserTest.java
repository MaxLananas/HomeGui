package com.maxlananas.homegui.core;

import com.maxlananas.homegui.core.HomeListParser.HomeListSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeListParserTest {

    private final HomeListParser parser = new HomeListParser();

    private static List<String> names(List<Home> homes) {
        return homes.stream().map(Home::name).collect(Collectors.toList());
    }

    // ------------------------------------------------------------ EssentialsX

    @Test
    void parsesTheEssentialsXInlineList() {
        assertEquals(List.of("base", "farm", "mine"),
                names(parser.parse("\u00A76Homes:\u00A7r \u00A7fbase\u00A7f, \u00A7ffarm\u00A7f, \u00A7fmine").orElseThrow()));
    }

    @Test
    void parsesTheEssentialsXCountedHeading() {
        assertEquals(List.of("base", "farm"),
                names(parser.parse("Homes (2): base, farm").orElseThrow()));
    }

    @Test
    void parsesTheBracketedVariant() {
        assertEquals(List.of("base", "farm"), names(parser.parse("Your homes: [base] [farm]").orElseThrow()));
    }

    @Test
    void parsesAPaginatedEssentialsXReplySpreadOverMessages() {
        HomeListSession session = parser.newSession();
        assertTrue(session.offer("----- Homes (1/2) -----").isEmpty());
        assertTrue(session.offer("base - world: 100, 64, 200").isEmpty());
        assertTrue(session.offer("farm - world_nether: -12, 40, 8").isEmpty());
        List<Home> homes = session.finish().orElseThrow();
        assertEquals(List.of("base", "farm"), names(homes));
        assertEquals(100, homes.get(0).coordinates().orElseThrow().x);
        assertEquals(-12, homes.get(1).coordinates().orElseThrow().x);
    }

    // -------------------------------------------------------------------- CMI

    @Test
    void parsesCmiOneHomePerLineAfterABareHeading() {
        HomeListSession session = parser.newSession();
        assertTrue(session.offer("Homes:").isEmpty());
        session.offer("base (world) 100,64,200");
        session.offer("farm (world_nether) -12,40,8");
        List<Home> homes = session.finish().orElseThrow();
        assertEquals(List.of("base", "farm"), names(homes));
        assertEquals("world", homes.get(0).world());
        assertTrue(homes.get(0).hasCoordinates());
    }

    @Test
    void parsesCmiLabelledLinesWithoutAHeading() {
        HomeListSession session = parser.newSession();
        session.offer("Home: base | world | 100 64 200");
        session.offer("Home: farm | world_nether | -12 40 8");
        List<Home> homes = session.finish().orElseThrow();
        assertEquals(List.of("base", "farm"), names(homes));
        assertEquals(100, homes.get(0).coordinates().orElseThrow().x);
    }

    // --------------------------------------------------------- shared shapes

    @Test
    void recognisesAnEmptyListInSeveralLanguages() {
        for (String line : new String[]{"No homes found.", "You have no homes!", "You dont have any homes",
                "Aucune maison.", "Keine Homes."}) {
            assertEquals(List.of(), parser.parse(line).orElseThrow(), "expected empty for: " + line);
        }
    }

    @Test
    void understandsHeadingsInEveryShippedLanguage() {
        for (String line : new String[]{"Homes: base", "Vos maisons : base", "Deine Homes: base",
                "Tus homes: base", "Suas homes: base", "I tuoi home: base", "Uw homes: base",
                "Twoje domy: base", "\u041C\u043e\u0438 \u0434\u043e\u043c\u0430: base"}) {
            assertEquals(List.of("base"), names(parser.parse(line).orElseThrow()), "expected a home for: " + line);
        }
    }

    @Test
    void keepsUnicodeNamesIntact() {
        assertEquals(List.of("Caf\u00E9", "\u5BB6", "\uB3D9\uC744"),
                names(parser.parse("Homes: Caf\u00E9, \u5BB6, \uB3D9\uC744").orElseThrow()));
    }

    @Test
    void ignoresUnrelatedChat() {
        assertTrue(parser.parse("[Server] Welcome home, player!").isEmpty());
        assertTrue(parser.parse("<Steve> anyone home?").isEmpty());
        assertTrue(parser.parse("HomeGui could not understand that").isEmpty());
    }

    @Test
    void dropsUnsafeNamesAndKeepsTheSafeOnes() {
        assertEquals(List.of("safe", "ok_name"),
                names(parser.parse("Homes: safe, /op, bad/name, ok_name, two words").orElseThrow()));
    }

    @Test
    void deduplicatesWhilePreservingTheFirstSeenCasing() {
        assertEquals(List.of("Base"), names(parser.parse("Homes: Base, base, BASE").orElseThrow()));
    }

    @Test
    void doesNotCollectOrdinaryChatWhileWaitingForAMultiLineReply() {
        HomeListSession session = parser.newSession();
        session.offer("Homes:");
        session.offer("base (world) 1, 2, 3");
        session.offer("hello there");
        assertEquals(List.of("base"), names(session.finish().orElseThrow()));
    }

    @Test
    void aHeadingClosesTheListWhenAnUnrelatedLineArrives() {
        HomeListSession session = parser.newSession();
        session.offer("Homes:");
        session.offer("base (world) 1, 2, 3");
        Optional<List<Home>> closed = session.offer("Someone joined the game");
        assertTrue(closed.isPresent());
        assertEquals(List.of("base"), names(closed.orElseThrow()));
    }

    @Test
    void pageMarkersDoNotEndTheList() {
        HomeListSession session = parser.newSession();
        session.offer("Homes (1/2):");
        session.offer("base (world) 1, 2, 3");
        assertTrue(session.offer("Page 1 of 2").isEmpty());
        session.offer("farm (world) 4, 5, 6");
        assertEquals(List.of("base", "farm"), names(session.finish().orElseThrow()));
    }

    @Test
    void refusesAnAbsurdlyLongLine() {
        StringBuilder builder = new StringBuilder("Homes: ");
        for (int i = 0; i < 400; i++) builder.append("name").append(i).append(", ");
        assertTrue(parser.parse(builder.toString()).isEmpty());
    }

    @Test
    void boundsTheNumberOfHomesItWillHold() {
        HomeListSession session = parser.newSession();
        session.offer("Homes:");
        for (int i = 0; i < HomeListParser.MAX_HOMES + 500; i++) {
            session.offer("home" + i + " (world) 1, 2, 3");
        }
        assertEquals(HomeListParser.MAX_HOMES, session.finish().orElseThrow().size());
    }

    @Test
    void stripsFormattingAndInvisibleCharactersBeforeMatching() {
        assertEquals("base", HomeListParser.normalise("\u00A76\u00A7lbase\u00A7r").strip());
        assertEquals(HomeListParser.LineKind.HEADING, HomeListParser.classify("\u00A7cHomes:\u00A7r base"));
    }

    @Test
    void classifiesLinesTheWayTheSessionExpects() {
        assertEquals(HomeListParser.LineKind.HEADING, HomeListParser.classify("Homes: base"));
        assertEquals(HomeListParser.LineKind.ENTRY, HomeListParser.classify("base (world) 1, 2, 3"));
        assertEquals(HomeListParser.LineKind.EMPTY_LIST, HomeListParser.classify("No homes."));
        assertEquals(HomeListParser.LineKind.PAGE_MARK, HomeListParser.classify("Page 1 of 2"));
        assertEquals(HomeListParser.LineKind.BARE_NAME, HomeListParser.classify("base"));
        assertEquals(HomeListParser.LineKind.OTHER, HomeListParser.classify("<Steve> hello"));
        assertEquals(HomeListParser.LineKind.BLANK, HomeListParser.classify("   "));
    }

    @Test
    void aCompletedSessionIgnoresLaterLines() {
        HomeListSession session = parser.newSession();
        session.offer("Homes: base");
        assertTrue(session.isClosed());
        assertTrue(session.offer("Homes: other").isEmpty());
        assertEquals(List.of("base"), names(session.finish().orElseThrow()));
    }

    @Test
    void finishingAnEmptySessionYieldsNothing() {
        assertFalse(parser.newSession().finish().isPresent());
    }
}
