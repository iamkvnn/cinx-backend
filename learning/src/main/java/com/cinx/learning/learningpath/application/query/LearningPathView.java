package com.cinx.learning.learningpath.application.query;

import com.cinx.learning.shared.domain.LearningPathStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathView {
    private String id;
    private String userId;
    private String title;
    private String description;
    private LearningPathStatus status;
    private Double currentProgress;
    private Integer totalItems;
    private Integer completedItems;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime droppedAt;
    private List<LearningPathItemView> items;
}
