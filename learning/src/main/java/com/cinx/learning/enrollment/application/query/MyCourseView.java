package com.cinx.learning.enrollment.application.query;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.shared.domain.CertificateStatus;

import java.time.LocalDateTime;

/** One course of "Học tập của tôi": the course, the caller's progress, the lesson to resume and the certificate. */
public record MyCourseView(
        String courseId,
        String title,
        String thumbnailUrl,
        Category category,
        Instructor instructor,
        LocalDateTime grantedAt,
        int totalItems,
        int completedItems,
        boolean completed,
        Boolean passed,
        Double avgScore,
        LocalDateTime lastActivityAt,
        Next next,
        boolean hasCertificate,
        Certificate certificate
) {
    public record Category(String id, String name) {
    }

    public record Instructor(String id, String name) {
    }

    public record Next(
            String lessonId,
            String lessonTitle,
            LessonType lessonType,
            int sectionPosition,
            int lessonPosition,
            Integer positionSeconds,
            int remainingMinutes
    ) {
    }

    public record Certificate(
            String id,
            CertificateStatus status,
            String certificateUrl,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt,
            LocalDateTime rejectedAt,
            String rejectionReason
    ) {
    }
}
