package com.cinx.learning.certificate.application.query;

import java.time.Instant;
import java.util.List;

public record CertificateConditionsView(
        String courseId,
        int minCompletionPercent,
        double minQuizScore,
        double minAssignmentScore,
        boolean autoApprove,
        Instant updatedAt
) {
    /** The saved conditions and how many pending requests they approved at once. */
    public record Saved(CertificateConditionsView conditions, int autoApproved) {
    }

    public record BatchResult(List<Result> results) {
    }

    public record Result(String requestId, boolean ok, String error) {
    }
}
