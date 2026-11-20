package com.cinx.learning.progress.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "LearningItemProgress")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"course_progress_id", "item_id"}))
public class LearningItemProgressJpaEntity extends BaseEntity {
    @Column(name = "item_id", nullable = false)
    private String itemId;
    private Boolean isCompleted;
    private Boolean isPassed;
    private Double score;
    /** UTC; set the first time the item becomes complete (null for completions before V16). */
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "course_progress_id", nullable = false)
    private CourseProgressJpaEntity courseProgress;
}
