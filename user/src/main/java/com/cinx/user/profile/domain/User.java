package com.cinx.user.profile.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A person's profile on the platform: who they are, how to reach them, and where they stand as an instructor
 * (application, verification, partnership). {@code id} is the storage key; {@code userId} is the identity every other
 * service knows the person by.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class User {
    private final String id;
    /** The persistence version (optimistic lock); null until the profile is first stored. */
    private Long version;
    private final String userId;
    private String email;
    private String name;
    private Gender gender;
    private Boolean isInstructorVerified;
    private String avatarFileKey;
    private String avatarUrl;
    private String phoneNumber;
    private String bio;
    private LocalDateTime lastAccessAt;
    private LocalDateTime instructorVerifiedAt;
    private LocalDateTime partnershipTerminatedAt;
    private PartnershipTerminationReasonType partnershipTerminationReasonType;
    private String partnershipTerminationReasonDetail;
    @Builder.Default
    private Boolean isReceivePushNotification = false;
    @Builder.Default
    private Boolean isPartnershipTerminated = false;
    /** Whether decorative animations play for this person (the owl, celebrations); interface motion is not affected. */
    @Builder.Default
    private Boolean effectsEnabled = true;
    private String cvFileKey;
    private String cvUrl;
    @Builder.Default
    private InstructorApplicationStatus instructorApplicationStatus = InstructorApplicationStatus.NONE;
    private LocalDateTime instructorApplicationSubmittedAt;
    private LocalDateTime instructorApplicationReviewedAt;
    private String instructorApplicationRejectionReason;
    private String onboardingIdempotencyKey;
    private String onboardingRequestFingerprint;
    /** Stamped by the store when the profile is created and changed; read-only here. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Applies the profile fields a person may change; a null argument leaves the stored value as it is. */
    public void applyProfileChanges(String name, Gender gender, String phoneNumber, String bio,
                                    Boolean isReceivePushNotification, String avatarFileKey, String cvFileKey) {
        if (name != null) this.name = name;
        if (gender != null) this.gender = gender;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (bio != null) this.bio = bio;
        if (isReceivePushNotification != null) this.isReceivePushNotification = isReceivePushNotification;
        if (avatarFileKey != null) this.avatarFileKey = avatarFileKey;
        if (cvFileKey != null) this.cvFileKey = cvFileKey;
    }

    /** Whether the person currently counts as a verified instructor: verified, and not cut off by a terminated partnership. */
    public boolean isActiveInstructor() {
        return Boolean.TRUE.equals(isInstructorVerified) && !Boolean.TRUE.equals(isPartnershipTerminated);
    }

    /** An application is under way, or the person is already verified. */
    public boolean hasActiveInstructorApplication() {
        return Boolean.TRUE.equals(isInstructorVerified) || instructorApplicationStatus == InstructorApplicationStatus.PENDING;
    }

    public void startInstructorApplication(LocalDateTime now) {
        instructorApplicationStatus = InstructorApplicationStatus.PENDING;
        instructorApplicationSubmittedAt = now;
    }

    public void approveAsInstructor(LocalDateTime now) {
        isInstructorVerified = true;
        instructorVerifiedAt = now;
        instructorApplicationStatus = InstructorApplicationStatus.APPROVED;
        instructorApplicationReviewedAt = now;
        instructorApplicationRejectionReason = null;
    }

    public void rejectInstructorApplication(String reason, LocalDateTime now) {
        isInstructorVerified = false;
        instructorApplicationStatus = InstructorApplicationStatus.REJECTED;
        instructorApplicationReviewedAt = now;
        instructorApplicationRejectionReason = reason == null ? null : reason.trim();
    }

    /** Ends the partnership: no longer verified, with the reason on record. A blank detail counts as none. */
    public void terminatePartnership(PartnershipTerminationReasonType reasonType, String reasonDetail, LocalDateTime now) {
        isInstructorVerified = false;
        isPartnershipTerminated = true;
        partnershipTerminatedAt = now;
        partnershipTerminationReasonType = reasonType;
        partnershipTerminationReasonDetail = reasonDetail == null || reasonDetail.isBlank() ? null : reasonDetail.trim();
    }

    /** Reopens an application after a rejection or a terminated partnership: the old verdict and reason are cleared. */
    public void resubmitInstructorApplication(LocalDateTime now) {
        instructorApplicationStatus = InstructorApplicationStatus.PENDING;
        instructorApplicationSubmittedAt = now;
        instructorApplicationReviewedAt = null;
        instructorApplicationRejectionReason = null;
        isPartnershipTerminated = false;
    }
}
