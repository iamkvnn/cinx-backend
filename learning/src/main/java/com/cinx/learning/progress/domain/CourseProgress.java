package com.cinx.learning.progress.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgress extends Model {
    private String userId;
    private String courseId;
    private Boolean isCompleted;
    private Boolean isPassed;
    private Double avgScore;
    private Integer totalItems;
    private Integer completedItems;
    private LocalDateTime completionTime;
}
