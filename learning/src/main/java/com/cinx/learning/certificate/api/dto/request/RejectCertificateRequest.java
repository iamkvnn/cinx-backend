package com.cinx.learning.certificate.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Body of a certificate rejection; the learner sees the reason, which is required (5–500 characters, checked in the service). */
public record RejectCertificateRequest(
        @Size(max = 500)
        @Schema(description = "Why the certificate is refused, shown to the learner", example = "Final assignment not passed yet, resubmit it to be reviewed")
        String reason
) {
}
