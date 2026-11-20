package com.cinx.learning.learningpath.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItem extends Model {
    private String learningPathId;
    private String courseId;
    private String lessonId;
    private Integer orderIndex;
    /** The named phase the lesson belongs to; null for a path without phases. */
    private String phase;
    private Boolean isSuggested;
    private Boolean isCompleted;
}
