package com.cinx.learning.assessment.domain;

import java.util.List;

/** Port: where AssignmentSubmissionAttachment rows are stored. */
public interface AssignmentSubmissionAttachmentRepository {
    List<AssignmentSubmissionAttachment> saveAll(Iterable<AssignmentSubmissionAttachment> entities);
}
