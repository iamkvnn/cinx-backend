package com.cinx.learning.community.domain;

import lombok.AllArgsConstructor;
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
public class CourseQuestion extends AuditedModel {
    private String courseId;
    private String lessonId;
    private String userId;
    private String title;
    private String content;
    private Integer upvoteCount = 0;
}
