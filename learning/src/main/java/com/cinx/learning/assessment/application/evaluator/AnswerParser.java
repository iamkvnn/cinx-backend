package com.cinx.learning.assessment.application.evaluator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for parsing the canonical answer format stored as JSON strings.
 *
 * <p><b>Contract</b>:
 * <ul>
 *   <li>SINGLE_CHOICE, MULTI_CHOICE, ORDERING — JSON array of option IDs: {@code ["id1","id2"]}</li>
 *   <li>SHORT_TEXT, ESSAY — JSON array of accepted texts: {@code ["Paris","paris","City of Light"]}</li>
 *   <li>MATCHING — JSON array of pair objects: {@code [{"optionId":"id1","matchText":"Paris"}]}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class AnswerParser {

    private final ObjectMapper MAPPER;

    public List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse answer as JSON string list: '{}'", json);
            return Collections.emptyList();
        }
    }

    public List<MatchingPair> parseMatchingPairs(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse answer as JSON matching pairs: '{}'", json);
            return Collections.emptyList();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class MatchingPair {
        private String optionId;
        private String matchText;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MatchingPair p)) return false;
            return Objects.equals(optionId, p.optionId)
                && Objects.equals(matchText, p.matchText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(optionId, matchText);
        }

        @Override
        public String toString() {
            return optionId + "→" + matchText;
        }
    }
}
