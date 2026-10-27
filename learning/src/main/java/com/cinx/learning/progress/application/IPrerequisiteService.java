package com.cinx.learning.progress.application;

import java.util.List;
import java.util.Map;

/**
 * Lesson prerequisites for a learner (operator: "Khoá theo tiên quyết", "Hoàn thành là mở").
 * A prerequisite is done when its progress item is completed, or, for a quiz, a session was submitted
 * (graded or pending grade), or, for an assignment, a submission exists: submitting counts, passing is not needed.
 */
public interface IPrerequisiteService {
    /** Prerequisite stable ids of this lesson the learner has not done yet; empty when the lesson is open. */
    List<String> missing(String userId, String courseId, String lessonId);

    /** Every locked published lesson of the course → its missing prerequisite ids; open lessons are left out. The caller must be able to read the course. */
    Map<String, List<String>> locks(String userId, String courseId);
}
