package com.cinx.learning.progress.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.progress.domain.CourseProgress;

/** What other features may read of a CourseProgress; a copy, so the domain class stays private to progress. */
@Value
@Builder
public class CourseProgressInfo {
    String id;
    Long version;
    String userId;
    String courseId;
    Boolean isCompleted;
    Boolean isPassed;
    Double avgScore;
    Integer totalItems;
    Integer completedItems;
    LocalDateTime completionTime;

    public static CourseProgressInfo of(CourseProgress domain) {
        return CourseProgressInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .userId(domain.getUserId())
                .courseId(domain.getCourseId())
                .isCompleted(domain.getIsCompleted())
                .isPassed(domain.getIsPassed())
                .avgScore(domain.getAvgScore())
                .totalItems(domain.getTotalItems())
                .completedItems(domain.getCompletedItems())
                .completionTime(domain.getCompletionTime())
                .build();
    }
}
