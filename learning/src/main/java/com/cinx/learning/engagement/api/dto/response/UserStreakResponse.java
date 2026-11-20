package com.cinx.learning.engagement.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakResponse {
    @Schema(example = "user_123")
    private String userId;
    @Schema(example = "5")
    private Integer currentStreak;
    @Schema(example = "15")
    private Integer highestStreak;
    @Schema(example = "2025-01-01")
    private LocalDate lastActivityDate;
    @Schema(example = "7", description = "The highest milestone (7, 30, 100) already celebrated; 0 when none")
    private Integer celebratedMilestone;
    @Schema(example = "12", nullable = true, description = "The count of a streak just broken and not yet shown as lost")
    private Integer lostStreak;
}
