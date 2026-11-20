package com.cinx.learning.enrollment.api.dto.response;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.shared.domain.CertificateStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** One course of "Học tập của tôi": the course, the caller's progress, the lesson to resume and the certificate. */
public record MyCourseResponse(
        @Schema(example = "course_abc123")
        String courseId,
        @Schema(example = "Spring Boot Microservices")
        String title,
        String thumbnailUrl,
        Category category,
        Instructor instructor,
        LocalDateTime grantedAt,
        @Schema(example = "25")
        int totalItems,
        @Schema(example = "11")
        int completedItems,
        boolean completed,
        Boolean passed,
        @Schema(example = "8.6")
        Double avgScore,
        LocalDateTime lastActivityAt,
        @Schema(description = "The lesson to resume; null once the course is completed")
        Next next,
        boolean hasCertificate,
        @Schema(description = "The caller's certificate request for this course, in any status; null before applying")
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
            @Schema(description = "1-based chapter position", example = "2")
            int sectionPosition,
            @Schema(description = "1-based position inside the chapter", example = "4")
            int lessonPosition,
            @Schema(description = "Where the video stopped; video lessons only", example = "252")
            Integer positionSeconds,
            @Schema(example = "9")
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
