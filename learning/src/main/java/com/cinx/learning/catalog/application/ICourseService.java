package com.cinx.learning.catalog.application;

import com.cinx.common.paging.PageResult;

import java.math.BigDecimal;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.command.CreateCourseCommand;
import com.cinx.learning.catalog.application.command.RejectCourseCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseCommand;
import com.cinx.learning.catalog.application.query.CourseReadiness;
import com.cinx.learning.catalog.application.query.CourseView;
import com.cinx.learning.catalog.application.query.InstructorCourseSummaryView;
import com.cinx.learning.catalog.application.query.RejectCourseView;
import com.cinx.learning.catalog.application.query.ReviewNoteView;

import java.util.List;

public interface ICourseService {
    CourseView getReadableCourseById(String currentUserId, String courseId);
    /** The public course page: see {@link ICourseAccessService#ensureViewableCourse}. */
    CourseView getPublicCourseById(String currentUserId, String courseId);
    /** The published (or archived) courses among the ids, as catalog cards: whether the reader may open them is not checked. */
    List<CourseView> getReadableCourseByIds(String currentUserId, List<String> courseIds);

    /** A published (or archived) course for another service, whatever its price: no reader's access applies. */
    CourseView getCourseForService(String courseId);

    /** The published (or archived) courses among the ids, in the order asked, for another service. */
    List<CourseView> getCoursesForService(List<String> courseIds);
    List<String> searchReadableCourseIds(List<String> courseIds, String query);
    List<String> getCourseIdsByInstructor(String instructorId);
    PageResult<CourseView> getAllPublishedCourses(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, Boolean hasCertificate, Boolean onSale, Long durationFrom, Long durationTo, int page, int size, String sort);
    List<CourseView> getPublishedCoursesInOrder(List<String> courseIds);
    CourseView getEditableDraftCourseById(String currentUserId, String courseId);
    /** What must be fixed before the course can be sent for review; submitting runs the same check. */
    CourseReadiness getReadiness(String currentUserId, String courseId);

    /** The draft as the instructor sees it, for a reviewer (review.any) who is not on the course team. */
    CourseView getDraftForReview(String courseId);

    CourseReadiness getReadinessForReview(String courseId);
    InstructorCourseSummaryView getInstructorCourseSummary(String instructorId);
    PageResult<CourseView> getAllCourses(String query, String categoryId, String instructorId, Integer rating, BigDecimal priceFrom, BigDecimal priceTo, CourseStatus status, CoursePublishStatus publishStatus, int page, int size, String sort);
    CourseView createCourse(String currentUserId, CreateCourseCommand request);
    CourseView updateCourse(String currentUserId, String courseId, UpdateCourseCommand request);
    CourseView submitCourse(String currentUserId, String courseId);
    /** Takes a course out of review, back to editing; the draft is untouched. */
    CourseView withdrawCourse(String currentUserId, String courseId);
    CourseView archiveCourse(String currentUserId, String courseId);
    CourseView unarchiveCourse(String currentUserId, String courseId);
    CourseView approveCourse(String courseId);
    CourseView rejectCourse(String courseId, RejectCourseCommand request);
    RejectCourseView getRejectReason(String currentUserId, String courseId);
    ReviewNoteView.Rejection getReviewNotes(String currentUserId, String courseId);
    void updateCourseRating(String courseId, Double rating);
    void updateCourseRatingStats(String courseId, Double rating, long ratingCount);
    void increaseEnrollmentCount(String courseId);
    void replayRecommendationEvents();
}

