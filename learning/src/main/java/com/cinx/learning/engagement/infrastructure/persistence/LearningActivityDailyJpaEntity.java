package com.cinx.learning.engagement.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "LearningActivityDaily")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id", "activity_date"}))
public class LearningActivityDailyJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String courseId;
    @Column(nullable = false)
    private LocalDate activityDate;
    @Column(nullable = false)
    private Long activeSeconds;
    @Column(nullable = false)
    private LocalDateTime lastActivityAt;
    /** Lesson (stable id) of the latest heartbeat that named one; drives "Học tiếp". */
    private String lastItemId;
}
