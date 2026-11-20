package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class WhisperLanguageRegistry {
    private static final Map<String, String> LANGUAGES = Map.ofEntries(
            Map.entry("en", "english"), Map.entry("zh", "chinese"), Map.entry("de", "german"),
            Map.entry("es", "spanish"), Map.entry("ru", "russian"), Map.entry("ko", "korean"),
            Map.entry("fr", "french"), Map.entry("ja", "japanese"), Map.entry("pt", "portuguese"),
            Map.entry("tr", "turkish"), Map.entry("pl", "polish"), Map.entry("ca", "catalan"),
            Map.entry("nl", "dutch"), Map.entry("ar", "arabic"), Map.entry("sv", "swedish"),
            Map.entry("it", "italian"), Map.entry("id", "indonesian"), Map.entry("hi", "hindi"),
            Map.entry("fi", "finnish"), Map.entry("vi", "vietnamese"), Map.entry("he", "hebrew"),
            Map.entry("uk", "ukrainian"), Map.entry("el", "greek"), Map.entry("ms", "malay"),
            Map.entry("cs", "czech"), Map.entry("ro", "romanian"), Map.entry("da", "danish"),
            Map.entry("hu", "hungarian"), Map.entry("ta", "tamil"), Map.entry("no", "norwegian"),
            Map.entry("th", "thai"), Map.entry("ur", "urdu"), Map.entry("hr", "croatian"),
            Map.entry("bg", "bulgarian"), Map.entry("lt", "lithuanian"), Map.entry("la", "latin"),
            Map.entry("mi", "maori"), Map.entry("ml", "malayalam"), Map.entry("cy", "welsh"),
            Map.entry("sk", "slovak"), Map.entry("te", "telugu"), Map.entry("fa", "persian"),
            Map.entry("lv", "latvian"), Map.entry("bn", "bengali"), Map.entry("sr", "serbian"),
            Map.entry("az", "azerbaijani"), Map.entry("sl", "slovenian"), Map.entry("kn", "kannada"),
            Map.entry("et", "estonian"), Map.entry("mk", "macedonian"), Map.entry("br", "breton"),
            Map.entry("eu", "basque"), Map.entry("is", "icelandic"), Map.entry("hy", "armenian"),
            Map.entry("ne", "nepali"), Map.entry("mn", "mongolian"), Map.entry("bs", "bosnian"),
            Map.entry("kk", "kazakh"), Map.entry("sq", "albanian"), Map.entry("sw", "swahili"),
            Map.entry("gl", "galician"), Map.entry("mr", "marathi"), Map.entry("pa", "punjabi"),
            Map.entry("si", "sinhala"), Map.entry("km", "khmer"), Map.entry("sn", "shona"),
            Map.entry("yo", "yoruba"), Map.entry("so", "somali"), Map.entry("af", "afrikaans"),
            Map.entry("oc", "occitan"), Map.entry("ka", "georgian"), Map.entry("be", "belarusian"),
            Map.entry("tg", "tajik"), Map.entry("sd", "sindhi"), Map.entry("gu", "gujarati"),
            Map.entry("am", "amharic"), Map.entry("yi", "yiddish"), Map.entry("lo", "lao"),
            Map.entry("uz", "uzbek"), Map.entry("fo", "faroese"), Map.entry("ht", "haitian creole"),
            Map.entry("ps", "pashto"), Map.entry("tk", "turkmen"), Map.entry("nn", "nynorsk"),
            Map.entry("mt", "maltese"), Map.entry("sa", "sanskrit"), Map.entry("lb", "luxembourgish"),
            Map.entry("my", "myanmar"), Map.entry("bo", "tibetan"), Map.entry("tl", "tagalog"),
            Map.entry("mg", "malagasy"), Map.entry("as", "assamese"), Map.entry("tt", "tatar"),
            Map.entry("haw", "hawaiian"), Map.entry("ln", "lingala"), Map.entry("ha", "hausa"),
            Map.entry("ba", "bashkir"), Map.entry("jw", "javanese"), Map.entry("su", "sundanese"),
            Map.entry("yue", "cantonese")
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("burmese", "my"), Map.entry("valencian", "ca"), Map.entry("flemish", "nl"),
            Map.entry("haitian", "ht"), Map.entry("letzeburgesch", "lb"), Map.entry("pushto", "ps"),
            Map.entry("panjabi", "pa"), Map.entry("moldavian", "ro"), Map.entry("moldovan", "ro"),
            Map.entry("sinhalese", "si"), Map.entry("castilian", "es"), Map.entry("mandarin", "zh")
    );

    public String normalize(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Subtitle language code is required");
        }
        String normalized = languageCode.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (ALIASES.containsKey(normalized)) {
            return ALIASES.get(normalized);
        }
        if (LANGUAGES.containsKey(normalized)) {
            return normalized;
        }
        throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Unsupported Whisper subtitle language: " + languageCode);
    }

    public String displayName(String languageCode) {
        String normalized = normalize(languageCode);
        String value = LANGUAGES.get(normalized);
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    public Set<String> supportedLanguageCodes() {
        return LANGUAGES.keySet();
    }
}

