package com.cinx.learning.community.application;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The words of a question worth a full-text match: three characters or more, as the index keeps, without the
 * Vietnamese words of how a question is asked. Left in, "làm thế nào" alone matched unrelated questions.
 */
final class QuestionSearchTerms {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_TERMS = 20;
    private static final Set<String> FUNCTION_WORDS = Set.of(
            "tại", "sao", "làm", "thế", "nào", "không", "được", "của", "cho", "với", "các", "những", "này", "khi",
            "thì", "mình", "bạn", "như", "cách", "hay", "hoặc", "vậy", "nên", "phải", "trong", "một", "cũng", "đang",
            "rồi", "giúp", "hỏi", "nhau", "khác", "chưa", "còn", "nhiều", "rất", "lại", "mới", "đâu", "luôn",
            "khong", "duoc", "nao", "lam", "nhu");

    private QuestionSearchTerms() {
    }

    /** The words joined by spaces, or an empty string when nothing worth matching is left. */
    static String of(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.codePointCount(0, word.length()) >= MIN_LENGTH)
                .filter(word -> !FUNCTION_WORDS.contains(word))
                .distinct()
                .limit(MAX_TERMS)
                .collect(Collectors.joining(" "));
    }
}
