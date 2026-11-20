package com.cinx.learning.certificate.domain;

import com.cinx.learning.shared.domain.CertificateStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest extends Model {
    private String userId;
    private String courseId;
    private CertificateStatus status;
    private String certificateUrl;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
}
