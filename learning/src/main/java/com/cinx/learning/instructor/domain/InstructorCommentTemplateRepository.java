package com.cinx.learning.instructor.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored InstructorCommentTemplate rows. */
public interface InstructorCommentTemplateRepository {
    List<InstructorCommentTemplate> findAllByInstructorIdAndKindOrderByCreatedAtDesc(String instructorId, CommentTemplateKind kind);

    long countByInstructorId(String instructorId);

    void delete(InstructorCommentTemplate entity);

    Optional<InstructorCommentTemplate> findById(String id);

    InstructorCommentTemplate save(InstructorCommentTemplate entity);
}
