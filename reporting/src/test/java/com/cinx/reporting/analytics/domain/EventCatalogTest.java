package com.cinx.reporting.analytics.domain;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventCatalogTest {
    private static final Path CONTRACT = Path.of("..", "contracts", "analytics", "product-events.yaml");

    @Test
    @SuppressWarnings("unchecked")
    void theJavaCatalogIsTheContractFile() throws IOException {
        Map<String, Object> contract;
        try (InputStream in = Files.newInputStream(CONTRACT)) {
            contract = new Yaml().load(in);
        }
        assertThat(contract.get("dictionaryVersion")).isEqualTo(EventCatalog.VERSION);
        Map<String, Map<String, Object>> events = (Map<String, Map<String, Object>>) contract.get("events");
        assertThat(events.keySet()).containsExactlyElementsOf(EventCatalog.names());

        events.forEach((name, event) -> {
            Map<String, Map<String, Object>> props = (Map<String, Map<String, Object>>) event.get("props");
            assertThat(props.keySet()).as(name).containsExactlyElementsOf(EventCatalog.props(name).keySet());
            props.forEach((prop, rule) -> {
                PropRule mine = EventCatalog.props(name).get(prop);
                String where = name + "." + prop;
                assertThat(mine.type().name().toLowerCase()).as(where).isEqualTo(rule.get("type"));
                assertThat(mine.required()).as(where).isEqualTo(Boolean.TRUE.equals(rule.get("required")));
                if (rule.containsKey("values")) assertThat(mine.values()).as(where).isEqualTo(rule.get("values"));
                if (rule.containsKey("maxLength")) assertThat(mine.maxLength()).as(where).isEqualTo(rule.get("maxLength"));
                if (rule.containsKey("min")) assertThat(mine.min()).as(where).isEqualTo(((Number) rule.get("min")).longValue());
                if (rule.containsKey("max")) assertThat(mine.max()).as(where).isEqualTo(((Number) rule.get("max")).longValue());
            });
        });
    }

    @Test
    void cleaningKeepsListedPropsAndDropsTheRest() {
        Map<String, Object> props = new HashMap<>();
        props.put("recommendationId", "r-1");
        props.put("courseId", "c-1");
        props.put("position", 2.0);
        props.put("email", "someone@example.test");
        props.put("rankerVersion", "free text with spaces");

        assertThat(EventCatalog.clean("recommendation_shown", props))
                .containsExactlyInAnyOrderEntriesOf(Map.of("recommendationId", "r-1", "courseId", "c-1", "position", 2L));
    }

    @Test
    void aRequiredPropMissingOrOutOfRangeRefusesTheEvent() {
        assertThatThrownBy(() -> EventCatalog.clean("recommendation_shown", Map.of("courseId", "c-1", "position", 1)))
                .isInstanceOf(InvalidProductEventException.class).hasMessageContaining("recommendationId");
        assertThatThrownBy(() -> EventCatalog.clean("recommendation_shown", Map.of("recommendationId", "r", "courseId", "c", "position", 100)))
                .isInstanceOf(InvalidProductEventException.class).hasMessageContaining("position");
        assertThatThrownBy(() -> EventCatalog.clean("learning_action_completed", Map.of("action", "cheated")))
                .isInstanceOf(InvalidProductEventException.class);
        assertThatThrownBy(() -> EventCatalog.clean("page_viewed", Map.of()))
                .isInstanceOf(InvalidProductEventException.class).hasMessageContaining("Unknown");
    }

    @Test
    void booleansAndKeysAreTyped() {
        assertThat(EventCatalog.clean("learning_action_completed", Map.of("action", "review_answered", "correct", "yes")))
                .doesNotContainKey("correct");
        assertThat(EventCatalog.clean("learning_action_completed", Map.of("action", "review_answered", "correct", true)))
                .containsEntry("correct", true);
        assertThatThrownBy(() -> EventCatalog.clean("experiment_exposed", Map.of("experimentKey", "Week Card", "variant", "control")))
                .isInstanceOf(InvalidProductEventException.class);
        assertThat(List.copyOf(EventCatalog.names())).contains(EventCatalog.EXPERIMENT_EXPOSED);
    }
}
