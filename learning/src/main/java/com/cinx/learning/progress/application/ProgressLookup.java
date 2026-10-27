package com.cinx.learning.progress.application;

import com.cinx.learning.progress.application.query.CourseProgressInfo;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** What the other features read of a learner's and a course's progress. */
public interface ProgressLookup {
    List<CourseProgressInfo> courseProgressOfUser(String userId);

    List<CourseProgressInfo> courseProgressOfUser(String userId, List<String> courseIds);

    /** The same rows as views, for the callers that hand them straight to the client. */
    List<CourseProgressView> courseProgressViewsOfUser(String userId, List<String> courseIds);

    List<CourseProgressInfo> courseProgressOfCourse(String courseId);

    List<CourseProgressInfo> courseProgressOfCourses(List<String> courseIds);

    List<LearningItemProgressInfo> itemProgressOfUser(String userId, String courseId);

    List<LearningItemProgressInfo> itemProgressOfUser(String userId, List<String> itemIds);

    Optional<LearningItemProgressInfo> itemProgress(String itemId, String userId);

    /** Learners with progress in each course, and how many of them completed it. */
    List<CompletionRow> completionPerCourse(Collection<String> courseIds);

    /** Learners who completed each item of one course. */
    List<ItemCount> completedPerItem(String courseId);

    /** Who passed these items of one course. */
    List<PassedRow> passedIn(String courseId, Collection<String> itemIds);

    /** Where the learner stopped in a video, in seconds. */
    Optional<Integer> videoPosition(String userId, String videoLessonId);

    record CompletionRow(String courseId, long total, Long completed) {
    }

    record ItemCount(String itemId, long total) {
    }

    record PassedRow(String itemId, String userId) {
    }
}
