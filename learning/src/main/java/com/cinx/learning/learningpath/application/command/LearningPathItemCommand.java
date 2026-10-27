package com.cinx.learning.learningpath.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemCommand {
    private String courseId;
    private String lessonId;
    private Integer orderIndex;
    private String phase;
    private Boolean isSuggested;
}
