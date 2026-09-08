package com.maxlananas.homegui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HomeListParserTest {
    private final HomeListParser parser = new HomeListParser();

    @Test void parsesCommonFormatsAndDeduplicates() {
        assertEquals(List.of("base", "Farm", "nether"), parser.parse("§6Homes: base, Farm | nether base").orElseThrow());
        assertEquals(List.of("base", "farm"), parser.parse("Your homes: [base] [farm]").orElseThrow());
    }

    @Test void recognizesAnEmptyList() {
        assertEquals(List.of(), parser.parse("No homes found.").orElseThrow());
    }

    @Test void ignoresUnrelatedChatAndUnsafeNames() {
        assertTrue(parser.parse("[Server] Welcome home, player!").isEmpty());
        assertEquals(List.of("safe"), parser.parse("Homes: safe, /op, bad/name").orElseThrow());
    }
}
