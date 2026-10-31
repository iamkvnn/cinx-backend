package com.cinx.learning.learningpath.api.dto.response;

import com.cinx.learning.shared.domain.LearningPathStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class LearningPathResponse {
    @Schema(example = "path_123")
    private String id;
    @Schema(example = "user_123")
    private String userId;
    @Schema(example = "Backend Developer Path")
    private String title;
    @Schema(example = "A path to become a complete backend developer.")
    private String description;
    @Schema(example = "ACTIVE")
    private LearningPathStatus status;
    @Schema(example = "50.0")
    private Double currentProgress;
    @Schema(example = "10")
    private Integer totalItems;
    @Schema(example = "5")
    private Integer completedItems;
    @Schema(description = "Null for paths created before dates were recorded")
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime droppedAt;
    private List<LearningPathItemResponse> items;
}
