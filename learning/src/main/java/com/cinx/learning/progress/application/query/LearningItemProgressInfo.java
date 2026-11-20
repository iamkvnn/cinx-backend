package com.cinx.learning.progress.application.query;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.progress.domain.LearningItemProgress;

/** What other features may read of a LearningItemProgress; a copy, so the domain class stays private to progress. */
@Value
@Builder
public class LearningItemProgressInfo {
    String id;
    Long version;
    String itemId;
    Boolean isCompleted;
    Boolean isPassed;
    Double score;
    LocalDateTime completedAt;
    String courseProgressId;

    public static LearningItemProgressInfo of(LearningItemProgress domain) {
        return LearningItemProgressInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .itemId(domain.getItemId())
                .isCompleted(domain.getIsCompleted())
                .isPassed(domain.getIsPassed())
                .score(domain.getScore())
                .completedAt(domain.getCompletedAt())
                .courseProgressId(domain.getCourseProgressId())
                .build();
    }
}
