package com.cinx.reporting.analytics.domain;

import java.util.List;
import java.util.Optional;

public interface ExperimentRepository {
    Optional<Experiment> findByKey(String key);

    List<Experiment> findAll();

    List<Experiment> findRunning();

    Experiment save(Experiment experiment);
}
