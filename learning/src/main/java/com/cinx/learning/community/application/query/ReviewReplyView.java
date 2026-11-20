package com.cinx.learning.community.application.query;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewReplyView {
    private String id;
    private String reviewId;
    private String instructorId;
    private UserSummaryView instructor;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
