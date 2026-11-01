package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.learning.shared.domain.CertificateStatus;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "CertificateRequest")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequestJpaEntity extends BaseEntity {
    private String userId;
    private String courseId;
    
    @Enumerated(EnumType.STRING)
    private CertificateStatus status;
    
    @Column(length = 1000)
    private String certificateUrl;
    
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime rejectedAt;
}