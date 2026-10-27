package com.cinx.learning.instructor.application.query;

import com.cinx.learning.shared.domain.LessonType;

import java.util.List;

/** "Học viên dừng ở đâu": how many learners completed each lesson, in course order. */
public record LessonReachView(long learners, List<Lesson> lessons) {
    /** lessonId is the stable id the player and Q&A use; percent has one decimal. */
    public record Lesson(String lessonId, int position, String sectionTitle, String title, LessonType type,
                         long completedCount, double percent, long questionCount) {
    }
}
