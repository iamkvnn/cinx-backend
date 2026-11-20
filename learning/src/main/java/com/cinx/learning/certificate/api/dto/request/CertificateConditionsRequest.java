package com.cinx.learning.certificate.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CertificateConditionsRequest(
        @NotNull @Min(1) @Max(100) Integer minCompletionPercent,
        @NotNull @DecimalMin("0") @DecimalMax("10") Double minQuizScore,
        @NotNull @DecimalMin("0") @DecimalMax("10") Double minAssignmentScore,
        @NotNull Boolean autoApprove
) {
}
