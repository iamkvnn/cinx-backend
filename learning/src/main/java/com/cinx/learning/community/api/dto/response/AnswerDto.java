package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnswerDto {
    @Schema(example = "ans_123")
    private String id;
    @Schema(example = "q_123")
    private String questionId;
    @Schema(example = "ans_098")
    private String parentAnswerId;
    @Schema(example = "user_123")
    private String userId;
    @Schema(description = "Author summary, resolved from the user service; null when it is unavailable")
    private UserSummaryResponse user;
    @Schema(example = "Polymorphism is the ability of an object to take on many forms.")
    private String content;
    @Schema(example = "false")
    private Boolean isInstructorAnswer;
    @Schema(example = "15")
    private Integer upvoteCount;
    @Schema(example = "true")
    private Boolean hasUpvoted;
    @Schema(example = "true")
    private Boolean markedHelpfulByCurrentUser;
    @Schema(example = "0")
    private Integer depth;
    @Schema(example = "2")
    private Integer repliesCount;
    @Schema(description = "The author started from an AI draft", example = "false")
    private Boolean aiAssisted;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
