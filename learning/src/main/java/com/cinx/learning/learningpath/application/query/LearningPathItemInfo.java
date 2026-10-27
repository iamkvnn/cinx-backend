package com.cinx.learning.learningpath.application.query;

import lombok.Builder;
import lombok.Value;
import com.cinx.learning.learningpath.domain.LearningPathItem;

/** What other features may read of a LearningPathItem; a copy, so the domain class stays private to learningpath. */
@Value
@Builder
public class LearningPathItemInfo {
    String id;
    Long version;
    String learningPathId;
    String courseId;
    String lessonId;
    Integer orderIndex;
    String phase;
    Boolean isSuggested;
    Boolean isCompleted;

    public static LearningPathItemInfo of(LearningPathItem domain) {
        return LearningPathItemInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .learningPathId(domain.getLearningPathId())
                .courseId(domain.getCourseId())
                .lessonId(domain.getLessonId())
                .orderIndex(domain.getOrderIndex())
                .phase(domain.getPhase())
                .isSuggested(domain.getIsSuggested())
                .isCompleted(domain.getIsCompleted())
                .build();
    }
}
