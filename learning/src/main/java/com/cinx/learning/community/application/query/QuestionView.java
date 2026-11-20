package com.cinx.learning.community.application.query;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionView {
    private String id;
    private String courseId;
    private String lessonId;
    private String userId;
    private UserSummaryView user;
    private String title;
    private String content;
    private Integer upvoteCount;
    private Boolean hasUpvoted;
    private Integer answersCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
