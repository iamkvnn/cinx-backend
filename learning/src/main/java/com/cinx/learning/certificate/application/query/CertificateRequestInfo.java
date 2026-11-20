package com.cinx.learning.certificate.application.query;

import com.cinx.learning.shared.domain.CertificateStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.certificate.domain.CertificateRequest;

/** What other features may read of a CertificateRequest; a copy, so the domain class stays private to certificate. */
@Value
@Builder
public class CertificateRequestInfo {
    String id;
    Long version;
    String userId;
    String courseId;
    CertificateStatus status;
    String certificateUrl;
    LocalDateTime requestedAt;
    LocalDateTime approvedAt;
    String rejectionReason;
    LocalDateTime rejectedAt;

    public static CertificateRequestInfo of(CertificateRequest domain) {
        return CertificateRequestInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .userId(domain.getUserId())
                .courseId(domain.getCourseId())
                .status(domain.getStatus())
                .certificateUrl(domain.getCertificateUrl())
                .requestedAt(domain.getRequestedAt())
                .approvedAt(domain.getApprovedAt())
                .rejectionReason(domain.getRejectionReason())
                .rejectedAt(domain.getRejectedAt())
                .build();
    }
}
