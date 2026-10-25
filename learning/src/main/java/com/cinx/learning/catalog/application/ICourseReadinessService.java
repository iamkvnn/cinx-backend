package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.LessonInfo;

import com.cinx.learning.catalog.application.query.CourseReadiness;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.Lesson;

import java.util.Collection;
import java.util.Set;

public interface ICourseReadinessService {
    /** Checks the draft (or the published copy when there is no draft). Submitting runs this same check. */
    CourseReadiness check(Course course);

    /** Stable ids of the lessons that have content: a video file, an article, a quiz question, an assignment brief. */
    Set<String> lessonsWithContent(Collection<Lesson> lessons);

    /** The same check for lessons another feature already holds. */
    Set<String> lessonsWithContentOf(Collection<LessonInfo> lessons);
}
