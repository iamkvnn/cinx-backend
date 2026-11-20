package com.cinx.learning.learningpath.domain;

import com.cinx.learning.shared.domain.LearningPathStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserLearningPath extends Model {
    private String userId;
    private String title;
    private String description;
    private LearningPathStatus status;
    private Double currentProgress;
    private Integer totalItems;
    private Integer completedItems;
    // Set when the create request carried an Idempotency-Key.
    private String idempotencyKey;
    private String requestFingerprint;
    // UTC, from the learning clock. Paths written before V20 have none.
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime droppedAt;
    @Builder.Default
    private List<LearningPathItem> items = new ArrayList<>();
}
