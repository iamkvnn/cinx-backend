package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.ReviewNoteStatus;
import com.cinx.learning.catalog.domain.ReviewNoteTarget;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/** A reviewer's note on one place of a course, sent to the instructor with the rejection. */
@Entity(name = "CourseReviewNote")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewNoteJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewNoteTarget target;

    private String lessonId;

    private Integer videoSecond;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewNoteStatus status;

    private LocalDateTime sentAt;
}
