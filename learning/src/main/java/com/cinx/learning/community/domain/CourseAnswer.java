package com.cinx.learning.community.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnswer extends AuditedModel {
    private String questionId;
    private String parentAnswerId;
    private String userId;
    private String content;
    private Boolean isInstructorAnswer = false;
    private Integer upvoteCount = 0;
    private Integer depth = 0;
    /** The author started from an AI draft; only ever turned on. */
    @Builder.Default
    private Boolean aiAssisted = false;
}
