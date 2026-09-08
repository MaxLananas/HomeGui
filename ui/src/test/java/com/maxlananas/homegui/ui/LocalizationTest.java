package com.maxlananas.homegui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationTest {

    private final Localization text = new Localization();

    @Test
    void everyShippedLocaleDefinesEveryKey() {
        assertEquals(java.util.List.of(), text.missingTranslations(),
                "a locale must not silently fall back to English");
    }

    @Test
    void everyLocaleHasTheSameKeyCountAsEnglish() {
        int expected = text.keys("en").size();
        assertTrue(expected > 60, "the interface has more strings than that");
        for (String code : Localization.SUPPORTED) {
            assertEquals(expected, text.keys(code).size(), code);
        }
    }

    @Test
    void theFallbackIsTheKeyItselfRatherThanAnEmptyString() {
        assertEquals("homegui.does.not.exist", text.get("homegui.does.not.exist"));
    }

    @Test
    void anUnsupportedLanguageFallsBackToEnglish() {
        text.setLanguage("xx");
        assertEquals("en", text.language());
        assertEquals("My homes", text.get("homegui.title.main"));
    }

    @Test
    void argumentsAreSubstituted() {
        text.setLanguage("en");
        assertEquals("12 homes", text.get("homegui.homes.count", 12));
        assertEquals("Teleporting to base", text.get("homegui.message.teleporting", "base"));
    }

    @Test
    void aBrokenFormatPatternDegradesToTheRawPattern() {
        Localization broken = new Localization();
        broken.setSource(new Localization.Source() {
            @Override
            public boolean has(String key) {
                return "homegui.homes.count".equals(key);
            }

            @Override
            public String get(String key) {
                return "%d %d %d";
            }
        });
        assertFalse(broken.get("homegui.homes.count", 3).isEmpty());
    }

    @Test
    void autoFollowsTheClientLanguage() {
        text.setLanguage(Localization.AUTO);
        text.setClientLocale("fr_FR");
        assertEquals("fr", text.language());
        text.setClientLocale("pt_BR");
        assertEquals("pt", text.language());
        text.setClientLocale("xx_YY");
        assertEquals("en", text.language());
    }

    @Test
    void clientTranslationsWinOverTheBuiltInTable() {
        Localization overridden = new Localization();
        overridden.setLanguage("en");
        overridden.setSource(new Localization.Source() {
            @Override
            public boolean has(String key) {
                return "homegui.title.main".equals(key);
            }

            @Override
            public String get(String key) {
                return "Resource pack title";
            }
        });
        assertEquals("Resource pack title", overridden.get("homegui.title.main"));
        assertFalse(overridden.get("homegui.button.close").isEmpty());
    }

    @Test
    void aThrowingTranslationSourceCannotBreakTheInterface() {
        Localization hostile = new Localization();
        hostile.setLanguage("en");
        hostile.setSource(new Localization.Source() {
            @Override
            public boolean has(String key) {
                throw new IllegalStateException("broken resource pack");
            }

            @Override
            public String get(String key) {
                throw new IllegalStateException("broken resource pack");
            }
        });
        assertEquals("Settings", hostile.get("homegui.tab.settings"));
    }

    @Test
    void cyclingReturnsToAuto() {
        text.setLanguage(Localization.AUTO);
        for (int i = 0; i < Localization.SUPPORTED.size() + 1; i++) text.cycle();
        assertEquals(Localization.AUTO, text.requestedLanguage());
    }

    @Test
    void localeFileNamesAreStable() {
        assertEquals("en_us", Localization.fileName("en"));
        assertEquals("zh_cn", Localization.fileName("zh_cn"));
        assertEquals("pt_br", Localization.fileName("pt"));
    }
}
