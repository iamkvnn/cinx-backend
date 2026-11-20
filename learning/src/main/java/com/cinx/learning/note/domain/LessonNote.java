package com.cinx.learning.note.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** A learner's note on a lesson of any type; the position is set for video lessons only. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LessonNote extends AuditedModel {
    private String userId;
    private String courseId;
    /** The lesson's stable id */
    private String lessonId;
    private String content;
    /** Seconds into the video; null for non-video lessons */
    private Integer positionSeconds;
}
