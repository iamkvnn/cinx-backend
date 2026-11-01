package com.cinx.learning.note.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A learner's note on a lesson of any type; the position is set for video lessons only. */
@Entity(name = "LessonNote")
@Table(
    name = "lesson_notes",
    indexes = {
        @Index(name = "idx_lesson_note_user_lesson", columnList = "userId, lessonId"),
        @Index(name = "idx_lesson_note_user_course", columnList = "userId, courseId")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LessonNoteJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String courseId;

    /** The lesson's stable id */
    @Column(nullable = false)
    private String lessonId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Seconds into the video; null for non-video lessons */
    private Integer positionSeconds;
}
