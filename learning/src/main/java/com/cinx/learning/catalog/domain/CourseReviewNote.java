package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.ReviewNoteStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.time.LocalDateTime;

/** A reviewer's note on one place of a course, sent to the instructor with the rejection. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewNote extends AuditedModel {
    private String courseId;
    private ReviewNoteTarget target;
    private String lessonId;
    private Integer videoSecond;
    private String content;
    private ReviewNoteStatus status;
    private LocalDateTime sentAt;
}
