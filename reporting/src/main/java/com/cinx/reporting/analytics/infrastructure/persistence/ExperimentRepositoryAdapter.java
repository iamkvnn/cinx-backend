package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.domain.Experiment;
import com.cinx.reporting.analytics.domain.ExperimentRepository;
import com.cinx.reporting.analytics.domain.ExperimentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExperimentRepositoryAdapter implements ExperimentRepository {
    private static final TypeReference<List<Experiment.Variant>> VARIANTS = new TypeReference<>() { };
    private static final TypeReference<Map<String, String>> FILTER = new TypeReference<>() { };

    private final ExperimentJpaRepository jpa;
    private final ObjectMapper json;

    @Override
    @Transactional(readOnly = true)
    public Optional<Experiment> findByKey(String key) {
        return key == null ? Optional.empty() : jpa.findById(key).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Experiment> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Experiment> findRunning() {
        return jpa.findByStatus(ExperimentStatus.RUNNING).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Experiment save(Experiment experiment) {
        ExperimentJpaEntity row = jpa.findById(experiment.getKey()).orElseGet(ExperimentJpaEntity::new);
        row.setKey(experiment.getKey());
        row.setDescription(experiment.getDescription());
        row.setStatus(experiment.getStatus());
        row.setVariants(write(experiment.getVariants()));
        row.setSalt(experiment.getSalt());
        row.setGoalEvent(experiment.getGoalEvent());
        row.setGoalFilter(write(experiment.getGoalFilter()));
        row.setGoalWindowDays(experiment.getGoalWindowDays());
        row.setStartedAt(experiment.getStartedAt());
        row.setStoppedAt(experiment.getStoppedAt());
        row.setUpdatedBy(experiment.getUpdatedBy());
        row.setCreatedAt(experiment.getCreatedAt());
        row.setUpdatedAt(experiment.getUpdatedAt());
        return toDomain(jpa.saveAndFlush(row));
    }

    private Experiment toDomain(ExperimentJpaEntity row) {
        return Experiment.builder()
                .key(row.getKey())
                .description(row.getDescription())
                .status(row.getStatus())
                .variants(read(row.getVariants(), VARIANTS))
                .salt(row.getSalt())
                .goalEvent(row.getGoalEvent())
                .goalFilter(read(row.getGoalFilter(), FILTER))
                .goalWindowDays(row.getGoalWindowDays())
                .startedAt(row.getStartedAt())
                .stoppedAt(row.getStoppedAt())
                .updatedBy(row.getUpdatedBy())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .version(row.getVersion())
                .build();
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Experiment JSON could not be written", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored experiment JSON is unreadable", e);
        }
    }
}
