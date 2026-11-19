package com.cinx.reporting.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An A/B experiment. Assignment is a pure function of the salt and the user, so it needs no row per user and never
 * changes while the experiment runs; anything but a running experiment gives everyone {@link #CONTROL}.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Experiment {
    public static final String CONTROL = "control";
    public static final int BUCKETS = 10_000;
    public static final int MAX_WINDOW_DAYS = 90;
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]{3,64}");
    private static final Pattern VARIANT = Pattern.compile("[a-z0-9_]{1,64}");

    private final String key;
    private final String description;
    private final ExperimentStatus status;
    private final List<Variant> variants;
    private final String salt;
    private final String goalEvent;
    /** Props the goal event must carry, e.g. {@code action = review_answered}. */
    private final Map<String, String> goalFilter;
    private final int goalWindowDays;
    private final LocalDateTime startedAt;
    private final LocalDateTime stoppedAt;
    private final String updatedBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long version;

    public static Experiment draft(String key, String description, List<Variant> variants, String goalEvent,
                                   Map<String, String> goalFilter, int goalWindowDays, String salt, String by,
                                   LocalDateTime now) {
        require(key != null && KEY.matcher(key).matches(), "The key is 3-64 of a-z, 0-9 and _");
        require(description != null && !description.isBlank() && description.length() <= 500, "The description is 1-500 characters");
        checkVariants(variants);
        require(EventCatalog.knows(goalEvent) && !EventCatalog.EXPERIMENT_EXPOSED.equals(goalEvent), "The goal is an event of the dictionary");
        Map<String, String> filter = goalFilter == null ? Map.of() : Map.copyOf(goalFilter);
        filter.forEach((prop, value) -> {
            PropRule rule = EventCatalog.props(goalEvent).get(prop);
            require(rule != null && rule.type() != PropType.BOOLEAN && rule.type() != PropType.INTEGER
                    && rule.accept(value).isPresent(), "The goal filter " + prop + " does not fit " + goalEvent);
        });
        require(goalWindowDays >= 1 && goalWindowDays <= MAX_WINDOW_DAYS, "The goal window is 1-" + MAX_WINDOW_DAYS + " days");
        return Experiment.builder()
                .key(key).description(description.trim()).status(ExperimentStatus.DRAFT)
                .variants(List.copyOf(variants)).salt(salt)
                .goalEvent(goalEvent).goalFilter(filter).goalWindowDays(goalWindowDays)
                .updatedBy(by).createdAt(now).updatedAt(now)
                .build();
    }

    public Experiment start(String by, LocalDateTime now) {
        require(status == ExperimentStatus.DRAFT, "Only a draft can start");
        return toBuilder().status(ExperimentStatus.RUNNING).startedAt(now).updatedBy(by).updatedAt(now).build();
    }

    /** The kill switch: everyone gets control at once. A stopped experiment never runs again; its results stay. */
    public Experiment stop(String by, LocalDateTime now) {
        require(status == ExperimentStatus.RUNNING, "Only a running experiment can stop");
        return toBuilder().status(ExperimentStatus.STOPPED).stoppedAt(now).updatedBy(by).updatedAt(now).build();
    }

    public String assign(String userId) {
        if (status != ExperimentStatus.RUNNING) {
            return CONTROL;
        }
        long bucket = Long.remainderUnsigned(hash(salt + ":" + userId), BUCKETS);
        long upTo = 0;
        for (Variant variant : variants) {
            upTo += (long) variant.weight() * BUCKETS / 100;
            if (bucket < upTo) return variant.key();
        }
        return variants.get(variants.size() - 1).key();
    }

    private static void checkVariants(List<Variant> variants) {
        require(variants != null && variants.size() >= 2 && variants.size() <= 5, "An experiment has 2-5 variants");
        var keys = new HashSet<String>();
        int total = 0;
        for (Variant variant : variants) {
            require(variant != null && variant.key() != null && VARIANT.matcher(variant.key()).matches(), "A variant key is 1-64 of a-z, 0-9 and _");
            require(keys.add(variant.key()), "Variant keys are unique");
            require(variant.weight() >= 1, "Every variant has a weight of at least 1");
            total += variant.weight();
        }
        require(keys.contains(CONTROL), "One variant is called control: what everyone sees when it is not running");
        require(total == 100, "The weights add up to 100");
    }

    private static long hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from the JVM", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new InvalidProductEventException(message);
    }

    public record Variant(String key, int weight) {
    }
}
