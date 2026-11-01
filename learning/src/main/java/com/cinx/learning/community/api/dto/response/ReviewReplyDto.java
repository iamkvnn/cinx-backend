package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewReplyDto {
    @Schema(example = "reply_123")
    private String id;
    @Schema(example = "rev_123")
    private String reviewId;
    @Schema(example = "inst_123")
    private String instructorId;
    private UserSummaryResponse instructor;
    @Schema(example = "Thank you for the review!")
    private String content;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
