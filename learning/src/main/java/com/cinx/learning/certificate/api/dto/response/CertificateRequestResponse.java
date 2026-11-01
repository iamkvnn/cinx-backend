package com.cinx.learning.certificate.api.dto.response;

import com.cinx.learning.shared.domain.CertificateStatus;
import java.time.LocalDateTime;

public record CertificateRequestResponse(
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
    public CertificateRequestResponse withChecks(CertificateChecks value) {
        return new CertificateRequestResponse(id, userId, courseId, status, certificateUrl, requestedAt, approvedAt,
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
