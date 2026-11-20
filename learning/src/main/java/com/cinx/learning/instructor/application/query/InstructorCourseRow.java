package com.cinx.learning.instructor.application.query;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.instructor.domain.CourseLifecycle;

import java.time.LocalDateTime;

/**
 * One course in the instructor's Khoá học list. learners, waiting and overdue are null for an EDITOR, who sees
 * content only (operator: "Chỉ Nội dung").
 */
public record InstructorCourseRow(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        String categoryName,
        CourseLifecycle lifecycle,
        boolean hasPendingChanges,
        CourseMembershipRole myRole,
        String ownerId,
        String ownerName,
        int lessonCount,
        int readyLessonCount,
        long durationMinutes,
        LocalDateTime publishedAt,
        LocalDateTime submittedAt,
        LocalDateTime archivedAt,
        LocalDateTime updatedAt,
        LocalDateTime lifecycleDate,
        Long learners,
        Long waiting,
        Long overdue,
        int reviewNoteCount,
        Double rating,
        long ratingCount
) {
}
