package com.maxlananas.homegui.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Interface text.
 *
 * <p>Translations live in {@code assets/homegui/lang/<locale>.json}, the same shape
 * Minecraft uses, so they are shipped in every artifact, can be overridden by a
 * resource pack, and can be checked for completeness by a test that simply compares
 * key sets. The old parallel-array table silently drifted: several languages were
 * missing their last few entries and fell back to English without anybody noticing.
 *
 * <p>{@link Source} lets the loader bridge hand over Minecraft's own translations
 * first, which is what makes resource packs and the client language work together
 * with the mod's own language setting.
 */
public final class Localization {

    /** Where the running client can offer a better translation than the built-in one. */
    public interface Source {
        boolean has(String key);

        String get(String key);
    }

    /** Language that follows the Minecraft client language. */
    public static final String AUTO = "auto";

    private static final String RESOURCE_ROOT = "/assets/homegui/lang/";
    private static final String FALLBACK = "en_us";

    /** Locales shipped with the mod, in the order the setting cycles through them. */
    public static final List<String> SUPPORTED = Collections.unmodifiableList(java.util.Arrays.asList(
            "en", "fr", "es", "de", "pt", "it", "nl", "pl", "ru", "uk", "tr", "cs", "sv", "ja", "ko", "zh_cn", "ar"));

    private static final Map<String, String> FILE_NAMES = buildFileNames();

    private final Map<String, Map<String, String>> cache = new HashMap<>();

    private String language = FALLBACK;
    private String requested = AUTO;
    private String clientLocale = FALLBACK;
    private Source source = null;

    private static Map<String, String> buildFileNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("en", "en_us");
        names.put("fr", "fr_fr");
        names.put("es", "es_es");
        names.put("de", "de_de");
        names.put("pt", "pt_br");
        names.put("it", "it_it");
        names.put("nl", "nl_nl");
        names.put("pl", "pl_pl");
        names.put("ru", "ru_ru");
        names.put("uk", "uk_ua");
        names.put("tr", "tr_tr");
        names.put("cs", "cs_cz");
        names.put("sv", "sv_se");
        names.put("ja", "ja_jp");
        names.put("ko", "ko_kr");
        names.put("zh_cn", "zh_cn");
        names.put("ar", "ar_sa");
        return Collections.unmodifiableMap(names);
    }

    /** Minecraft locale file name for one of the supported language codes. */
    public static String fileName(String code) {
        return FILE_NAMES.getOrDefault(code, FALLBACK);
    }

    /** Language code for a Minecraft locale such as {@code pt_BR}, or null when unsupported. */
    public static String codeForLocale(String locale) {
        if (locale == null) return null;
        String lowered = locale.toLowerCase(Locale.ROOT);
        if (FILE_NAMES.containsKey(lowered)) return lowered;
        int separator = lowered.indexOf('_');
        if (separator > 0) {
            String language = lowered.substring(0, separator);
            if (FILE_NAMES.containsKey(language)) return language;
        }
        return null;
    }

    /** Endonym shown in the language setting. Deliberately not translated. */
    public static String displayName(String code) {
        switch (code) {
            case AUTO: return "Auto";
            case "en": return "English";
            case "fr": return "Fran\u00e7ais";
            case "es": return "Espa\u00f1ol";
            case "de": return "Deutsch";
            case "pt": return "Portugu\u00eas";
            case "it": return "Italiano";
            case "nl": return "Nederlands";
            case "pl": return "Polski";
            case "ru": return "\u0420\u0443\u0441\u0441\u043a\u0438\u0439";
            case "uk": return "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430";
            case "tr": return "T\u00fcrk\u00e7e";
            case "cs": return "\u010ce\u0161tina";
            case "sv": return "Svenska";
            case "ja": return "\u65e5\u672c\u8a9e";
            case "ko": return "\ud55c\uad6d\uc5b4";
            case "zh_cn": return "\u7b80\u4f53\u4e2d\u6587";
            case "ar": return "\u0627\u0644\u0639\u0631\u0628\u064a\u0629";
            default: return code;
        }
    }

    public void setSource(Source source) {
        this.source = source;
    }

    /** Sets the mod's language, where {@value #AUTO} follows the client. */
    public void setLanguage(String code) {
        requested = code == null || code.isEmpty() ? AUTO : code.toLowerCase(Locale.ROOT);
        resolve();
    }

    /** Feeds the client language in, used while {@link #AUTO} is selected. */
    public void setClientLocale(String locale) {
        clientLocale = locale == null ? FALLBACK : locale;
        resolve();
    }

    private void resolve() {
        if (AUTO.equals(requested)) {
            String mapped = codeForLocale(clientLocale);
            language = mapped == null ? "en" : mapped;
            return;
        }
        language = SUPPORTED.contains(requested) ? requested : "en";
    }

    public String language() { return language; }

    public String requestedLanguage() { return requested; }

    /** Cycles to the next language, wrapping from the last back to auto. */
    public String cycle() {
        List<String> order = new ArrayList<>();
        order.add(AUTO);
        order.addAll(SUPPORTED);
        int index = order.indexOf(requested);
        String next = order.get(Math.floorMod(index + 1, order.size()));
        setLanguage(next);
        return next;
    }

    /** Looks a key up: client translation first, then the built-in table, then the key. */
    public String get(String key) {
        if (source != null) {
            try {
                if (source.has(key)) return source.get(key);
            } catch (RuntimeException ignored) {
                // a broken resource pack must not break the interface
            }
        }
        String value = table(language).get(key);
        if (value != null) return value;
        return table("en").getOrDefault(key, key);
    }

    /** Formats a translation with {@link String#format(Locale, String, Object...)}. */
    public String get(String key, Object... arguments) {
        String pattern = get(key);
        if (arguments == null || arguments.length == 0) return pattern;
        try {
            return String.format(Locale.ROOT, pattern, arguments);
        } catch (RuntimeException ignored) {
            return pattern;
        }
    }

    /** Keys present in one locale. Used by the completeness test and by tooling. */
    public Set<String> keys(String code) {
        return table(code).keySet();
    }

    /** True when every supported locale defines every key the fallback defines. */
    public List<String> missingTranslations() {
        Set<String> reference = table("en").keySet();
        List<String> missing = new ArrayList<>();
        for (String code : SUPPORTED) {
            Set<String> keys = table(code).keySet();
            for (String key : reference) {
                if (!keys.contains(key)) missing.add(code + ":" + key);
            }
        }
        return missing;
    }

    private Map<String, String> table(String code) {
        String fileName = fileName(code);
        return cache.computeIfAbsent(fileName, Localization::readTable);
    }

    private static Map<String, String> readTable(String fileName) {
        Map<String, String> values = new LinkedHashMap<>();
        String resource = RESOURCE_ROOT + fileName + ".json";
        try (InputStream stream = Localization.class.getResourceAsStream(resource)) {
            if (stream == null) return values;
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) text.append(line).append('\n');
            }
            JsonElement parsed = JsonParser.parseString(text.toString());
            if (!parsed.isJsonObject()) return values;
            JsonObject object = parsed.getAsJsonObject();
            Set<String> keys = new LinkedHashSet<>(object.keySet());
            for (String key : keys) {
                JsonElement element = object.get(key);
                if (element != null && element.isJsonPrimitive()) values.put(key, element.getAsString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (RuntimeException ignored) {
            // a malformed translation file degrades to English rather than crashing
        }
        return values;
    }
}
