package com.cinx.learning.progress.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "CourseProgress")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class CourseProgressJpaEntity extends BaseEntity {
    private String userId;
    private String courseId;
    private Boolean isCompleted;
    private Boolean isPassed;
    private Double avgScore;
    private Integer totalItems;
    private Integer completedItems;
    private LocalDateTime completionTime;

    @OneToMany(mappedBy = "courseProgress")
    private List<LearningItemProgressJpaEntity> learningItemProgressList;
}
