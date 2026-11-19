package com.cinx.reporting.analytics.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The closed event dictionary v1. It mirrors {@code contracts/analytics/product-events.yaml}; a test keeps the two equal.
 */
public final class EventCatalog {
    public static final int VERSION = 1;
    public static final String EXPERIMENT_EXPOSED = "experiment_exposed";
    private static final String[] SURFACES = {"launcher", "drawer", "tutor", "tutor_sheet", "path_workspace"};
    private static final Map<String, Map<String, PropRule>> EVENTS = build();

    private EventCatalog() {
    }

    public static boolean knows(String name) {
        return EVENTS.containsKey(name);
    }

    public static Set<String> names() {
        return EVENTS.keySet();
    }

    public static Map<String, PropRule> props(String name) {
        Map<String, PropRule> props = EVENTS.get(name);
        if (props == null) {
            throw new InvalidProductEventException("Unknown event: " + name);
        }
        return props;
    }

    /** The props kept for one event: listed ones that fit their rule; a required prop that is missing or wrong refuses it. */
    public static Map<String, Object> clean(String name, Map<String, Object> props) {
        Map<String, Object> given = props == null ? Map.of() : props;
        Map<String, Object> kept = new LinkedHashMap<>();
        props(name).forEach((prop, rule) -> {
            Object value = given.get(prop);
            if (value == null) {
                if (rule.required()) throw new InvalidProductEventException(name + " needs " + prop);
                return;
            }
            Object accepted = rule.accept(value).orElse(null);
            if (accepted == null && rule.required()) {
                throw new InvalidProductEventException(name + "." + prop + " has a value its rule does not allow");
            }
            if (accepted != null) kept.put(prop, accepted);
        });
        return kept;
    }

    private static Map<String, Map<String, PropRule>> build() {
        Map<String, Map<String, PropRule>> events = new LinkedHashMap<>();
        events.put("recommendation_shown", props(
                "recommendationId", PropRule.id(true),
                "courseId", PropRule.id(true),
                "position", PropRule.integer(0, 99, true),
                "rankerVersion", PropRule.string(64)));
        events.put("recommendation_clicked", props(
                "recommendationId", PropRule.id(true),
                "courseId", PropRule.id(true),
                "position", PropRule.integer(0, 99, false)));
        events.put("ai_feature_exposed", props(
                "surface", PropRule.oneOf(true, SURFACES),
                "placement", PropRule.string(64)));
        events.put("ai_request_started", props(
                "surface", PropRule.oneOf(true, SURFACES),
                "placement", PropRule.string(64),
                "mode", PropRule.string(32)));
        events.put("ai_request_completed", props(
                "surface", PropRule.oneOf(true, SURFACES),
                "turnId", PropRule.id(false),
                "capabilityId", PropRule.string(96),
                "durationMs", PropRule.integer(0, 3_600_000, false)));
        events.put("ai_request_failed", props(
                "surface", PropRule.oneOf(true, SURFACES),
                "stage", PropRule.oneOf(true, "request", "stream", "stopped"),
                "code", PropRule.string(64)));
        events.put("ai_feedback_submitted", props(
                "turnId", PropRule.id(true),
                "reason", PropRule.string(32)));
        events.put("learning_action_completed", props(
                "action", PropRule.oneOf(true, "lesson_completed", "quiz_submitted", "review_answered", "study_plan_set"),
                "courseId", PropRule.id(false),
                "lessonId", PropRule.id(false),
                "correct", PropRule.bool()));
        events.put(EXPERIMENT_EXPOSED, props(
                "experimentKey", PropRule.key(true),
                "variant", PropRule.key(true)));
        return Collections.unmodifiableMap(events);
    }

    private static Map<String, PropRule> props(Object... pairs) {
        Map<String, PropRule> props = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            props.put((String) pairs[i], (PropRule) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(props);
    }
}
