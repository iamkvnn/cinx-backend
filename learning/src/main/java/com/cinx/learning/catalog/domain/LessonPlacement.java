package com.cinx.learning.catalog.domain;

/** The course that holds a copy of a lesson, draft or published, and the lesson's title in that copy. */
public record LessonPlacement(String courseId, String lessonTitle) {
}
