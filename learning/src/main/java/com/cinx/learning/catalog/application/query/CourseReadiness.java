package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.LessonType;

import java.util.List;

/** What the server checks before a course may be sent for review; the editor's rail shows the same list. */
public record CourseReadiness(
        boolean ready,
        int totalLessons,
        int readyLessons,
        List<Problem> problems
) {
    public enum ProblemCode {
        DESCRIPTION_MISSING,
        COVER_MISSING,
        PRICE_INVALID,
        NO_LESSON,
        LESSON_EMPTY
    }

    /** {@code lessonId} is the lesson's stable id; the lesson fields are set for LESSON_EMPTY only. */
    public record Problem(
            ProblemCode code,
            String lessonId,
            String lessonTitle,
            LessonType lessonType
    ) {
        public static Problem course(ProblemCode code) {
            return new Problem(code, null, null, null);
        }

        public static Problem lesson(String lessonId, String lessonTitle, LessonType lessonType) {
            return new Problem(ProblemCode.LESSON_EMPTY, lessonId, lessonTitle, lessonType);
        }
    }
}
