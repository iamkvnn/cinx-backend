package com.cinx.learning.certificate.application.command;

public record CertificateConditionsCommand(
           Integer minCompletionPercent,
           Double minQuizScore,
           Double minAssignmentScore,
         Boolean autoApprove
) {
}
