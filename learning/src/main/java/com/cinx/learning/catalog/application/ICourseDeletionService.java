package com.cinx.learning.catalog.application;

public interface ICourseDeletionService {
    /**
     * Deletes a draft course forever: the owner only, never published, not in review, no learner.
     * Rows go in one transaction; the course's S3 objects are deleted best-effort after the commit.
     */
    void deleteDraftCourse(String currentUserId, String courseId);
}
