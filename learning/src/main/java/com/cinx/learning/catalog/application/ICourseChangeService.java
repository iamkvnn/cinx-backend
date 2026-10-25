package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.DraftChanges;

public interface ICourseChangeService {
    /** The draft against the published version. Every list is empty when there is no draft or no live version. */
    DraftChanges getDraftChanges(String currentUserId, String courseId);

    /** The same changes, for a reviewer (review.any) who is not on the course team. */
    DraftChanges getDraftChangesForReview(String courseId);
}
