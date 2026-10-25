package com.cinx.learning.catalog.domain;

import java.util.List;

/** Port: where AssignmentAttachment rows are stored. */
public interface AssignmentAttachmentRepository {
    void deleteAll(Iterable<AssignmentAttachment> entities);

    List<AssignmentAttachment> saveAll(Iterable<AssignmentAttachment> entities);
}
