package com.cinx.reporting.analytics.infrastructure.persistence;

import com.cinx.reporting.analytics.domain.ExperimentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "experiment")
@Getter
@Setter
@NoArgsConstructor
class ExperimentJpaEntity {
    @Id
    @Column(name = "experiment_key", length = 64)
    private String key;

    @Version
    private Long version;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status;

    /** JSON array of {key, weight}. */
    @Column(nullable = false, length = 1000)
    private String variants;

    @Column(nullable = false, length = 64)
    private String salt;

    @Column(name = "goal_event", nullable = false, length = 64)
    private String goalEvent;

    /** JSON object of prop to value. */
    @Column(name = "goal_filter", nullable = false, length = 1000)
    private String goalFilter;

    @Column(name = "goal_window_days", nullable = false)
    private int goalWindowDays;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
