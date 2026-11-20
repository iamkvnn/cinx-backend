package com.cinx.learning.instructor.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** A comment an instructor saved to reuse ("Nhận xét mẫu"). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorCommentTemplate extends AuditedModel {
    private String instructorId;
    private CommentTemplateKind kind;
    private String content;
}
