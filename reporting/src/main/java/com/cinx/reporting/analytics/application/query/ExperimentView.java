package com.cinx.reporting.analytics.application.query;

import com.cinx.reporting.analytics.domain.Experiment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ExperimentView(String key, String description, String status, List<VariantWeightView> variants,
                             String goalEvent, Map<String, String> goalFilter, int goalWindowDays,
                             LocalDateTime startedAt, LocalDateTime stoppedAt, String updatedBy,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ExperimentView of(Experiment experiment) {
        return new ExperimentView(experiment.getKey(), experiment.getDescription(), experiment.getStatus().name(),
                experiment.getVariants().stream().map(v -> new VariantWeightView(v.key(), v.weight())).toList(),
                experiment.getGoalEvent(), experiment.getGoalFilter(), experiment.getGoalWindowDays(),
                experiment.getStartedAt(), experiment.getStoppedAt(), experiment.getUpdatedBy(),
                experiment.getCreatedAt(), experiment.getUpdatedAt());
    }

    public record VariantWeightView(String key, int weight) {
    }
}
