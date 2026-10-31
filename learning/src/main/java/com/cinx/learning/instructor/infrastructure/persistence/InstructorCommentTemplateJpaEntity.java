package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.instructor.domain.CommentTemplateKind;

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

/** A comment an instructor saved to reuse ("Nhận xét mẫu"). */
@Entity(name = "InstructorCommentTemplate")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorCommentTemplateJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String instructorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommentTemplateKind kind;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
