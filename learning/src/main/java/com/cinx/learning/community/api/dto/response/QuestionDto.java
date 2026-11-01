package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionDto {
    @Schema(example = "q_123")
    private String id;
    @Schema(example = "course_123")
    private String courseId;
    @Schema(example = "les_123")
    private String lessonId;
    @Schema(example = "user_123")
    private String userId;
    @Schema(description = "Author summary, resolved from the user service; null when it is unavailable")
    private UserSummaryResponse user;
    @Schema(example = "How does polymorphism work?")
    private String title;
    @Schema(example = "Can someone explain polymorphism with an example?")
    private String content;
    @Schema(example = "10")
    private Integer upvoteCount;
    @Schema(example = "true")
    private Boolean hasUpvoted;
    @Schema(example = "5")
    private Integer answersCount;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
    @Schema(example = "2025-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
