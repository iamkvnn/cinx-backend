package com.cinx.learning.certificate.application.query;

import com.cinx.learning.shared.domain.CertificateStatus;
import java.time.LocalDateTime;

public record CertificateRequestView(
        String id,
        String userId,
        String courseId,
        CertificateStatus status,
        String certificateUrl,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        String rejectionReason,
        LocalDateTime rejectedAt,
        CertificateChecks checks
) {
    /** The same request with the course's condition checks attached (instructor lists only). */
    public CertificateRequestView withChecks(CertificateChecks value) {
        return new CertificateRequestView(id, userId, courseId, status, certificateUrl, requestedAt, approvedAt,
                rejectionReason, rejectedAt, value);
    }

    /** How a learner stands against the course's certificate conditions. */
    public record CertificateChecks(
            int completionPercent,
            int lessonsDone,
            int lessonsTotal,
            Double minQuizScore,
            String failingQuiz,
            Double minAssignmentScore,
            String failingAssignment,
            boolean eligible
    ) {
    }
}
