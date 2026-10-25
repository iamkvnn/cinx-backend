package com.cinx.learning.shared.application;

import java.util.Collection;

/**
 * Lets a feature remove what it keeps about a course the catalog is deleting. It runs inside the delete transaction,
 * before the course's own rows go, so everything is removed or nothing is.
 */
public interface CourseDeletionHook {
    void onCourseDeleted(String courseId, Collection<String> lessonStableIds);
}
