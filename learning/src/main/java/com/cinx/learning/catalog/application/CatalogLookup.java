package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.AssignmentLessonInfo;
import com.cinx.learning.catalog.application.query.CourseDraftInfo;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseInstructorMembershipInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.query.VideoLessonInfo;
import com.cinx.learning.shared.domain.CourseStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** What the other features read of courses, their lessons and who works on them. */
public interface CatalogLookup {
    Optional<CourseInfo> course(String courseId);

    List<CourseInfo> courses(Collection<String> courseIds);

    /** Only the published ones among these ids. */
    List<CourseInfo> publishedCourses(List<String> courseIds);

    /** The courses the instructor owns, not the ones they are only a member of. */
    List<String> courseIdsOfInstructor(String instructorId);

    long countCoursesByStatus(CourseStatus status);

    long countInstructorsOfPublishedCourses();

    Optional<CourseDraftInfo> draftOfCourse(String courseId);

    List<CourseInstructorMembershipInfo> activeMembershipsOf(String instructorId);

    /** Category names by id; an id nobody knows is left out. */
    Map<String, String> categoryNames(Collection<String> categoryIds);

    /** The readable (published or archived) lessons of a course, in curriculum order. */
    List<LessonInfo> readableLessons(String courseId);

    List<LessonInfo> readableLessonsOfCourses(Collection<String> courseIds);

    List<LessonInfo> readableLessonsByStableIds(Collection<String> stableIds);

    List<LessonInfo> readableLessonsByStableId(String courseId, String stableId);

    List<LessonInfo> publishedLessons(String courseId);

    List<LessonInfo> draftLessons(String draftId);

    Optional<VideoLessonInfo> videoLesson(String lessonId);

    List<VideoLessonInfo> videoLessons(Collection<String> lessonIds);

    Optional<AssignmentLessonInfo> assignmentLesson(String lessonId);

    /** Question counts per quiz lesson stable id; a quiz without questions is left out. */
    Map<String, Long> quizQuestionCounts(Collection<String> quizLessonIds);

    /** Notes the reviewers sent back to the instructor and that still wait for a fix. */
    int sentReviewNoteCount(String courseId);

    /** When the latest rejection of the course was recorded, if it was rejected. */
    Optional<LocalDateTime> lastRejectedAt(String courseId);
}
