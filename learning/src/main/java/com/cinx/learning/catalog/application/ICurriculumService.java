package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseCurriculumView;

public interface ICurriculumService {
    CourseCurriculumView getReadableCurriculum(String currentUserId, String courseId);
    /** The public course page: the published outline of a course the viewer may see the intro of. */
    CourseCurriculumView getPublicCurriculum(String currentUserId, String courseId);
    CourseCurriculumView getEnrolledCurriculum(String courseId);
    CourseCurriculumView getEditableDraftCurriculum(String currentUserId, String courseId);

    /** The draft curriculum, for a reviewer (review.any) who is not on the course team. */
    CourseCurriculumView getDraftCurriculumForReview(String courseId);
}

