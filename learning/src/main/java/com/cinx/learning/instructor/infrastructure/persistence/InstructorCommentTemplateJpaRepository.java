package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.instructor.domain.CommentTemplateKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface InstructorCommentTemplateJpaRepository extends JpaRepository<InstructorCommentTemplateJpaEntity, String> {
    List<InstructorCommentTemplateJpaEntity> findAllByInstructorIdAndKindOrderByCreatedAtDesc(String instructorId, CommentTemplateKind kind);

    long countByInstructorId(String instructorId);
}
