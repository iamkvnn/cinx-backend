package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.common.model.AuditableEntity;
import com.cinx.user.profile.domain.Gender;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import com.cinx.user.profile.domain.PartnershipTerminationReasonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity extends AuditableEntity {
    private String email;
    private String name;
    private Gender gender;
    private Boolean isInstructorVerified;
    private String avatarFileKey;
    private String avatarUrl;
    private String phoneNumber;
    @Column(length = 2000)
    private String bio;
    private LocalDateTime lastAccessAt;
    private LocalDateTime instructorVerifiedAt;
    private LocalDateTime partnershipTerminatedAt;
    @Enumerated(EnumType.STRING)
    private PartnershipTerminationReasonType partnershipTerminationReasonType;
    @Column(length = 2000)
    private String partnershipTerminationReasonDetail;
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean isReceivePushNotification = false;
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean isPartnershipTerminated = false;
    private String userId;
    private String cvFileKey;
    private String cvUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InstructorApplicationStatus instructorApplicationStatus = InstructorApplicationStatus.NONE;
    private LocalDateTime instructorApplicationSubmittedAt;
    private LocalDateTime instructorApplicationReviewedAt;
    @Column(length = 2000)
    private String instructorApplicationRejectionReason;
    @Column(length = 255)
    private String onboardingIdempotencyKey;
    @Column(length = 64)
    private String onboardingRequestFingerprint;
}
