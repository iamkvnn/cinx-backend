package com.cinx.learning.learningpath.application.query;

import com.cinx.learning.shared.domain.LearningPathStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.learningpath.domain.UserLearningPath;

/** What other features may read of a UserLearningPath; a copy, so the domain class stays private to learningpath. */
@Value
@Builder
public class UserLearningPathInfo {
    String id;
    Long version;
    String userId;
    String title;
    String description;
    LearningPathStatus status;
    Double currentProgress;
    Integer totalItems;
    Integer completedItems;
    LocalDateTime createdAt;
    LocalDateTime completedAt;
    LocalDateTime droppedAt;
    List<LearningPathItemInfo> items;

    public static UserLearningPathInfo of(UserLearningPath domain) {
        return UserLearningPathInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .currentProgress(domain.getCurrentProgress())
                .totalItems(domain.getTotalItems())
                .completedItems(domain.getCompletedItems())
                .createdAt(domain.getCreatedAt())
                .completedAt(domain.getCompletedAt())
                .droppedAt(domain.getDroppedAt())
                .items(domain.getItems() == null ? null : domain.getItems().stream().map(LearningPathItemInfo::of).toList())
                .build();
    }
}
