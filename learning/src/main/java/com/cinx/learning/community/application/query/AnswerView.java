package com.cinx.learning.community.application.query;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnswerView {
    private String id;
    private String questionId;
    private String parentAnswerId;
    private String userId;
    private UserSummaryView user;
    private String content;
    private Boolean isInstructorAnswer;
    private Integer upvoteCount;
    private Boolean hasUpvoted;
    private Boolean markedHelpfulByCurrentUser;
    private Integer depth;
    private Integer repliesCount;
    private Boolean aiAssisted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
