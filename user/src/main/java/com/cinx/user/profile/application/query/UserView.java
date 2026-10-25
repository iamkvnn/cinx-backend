package com.cinx.user.profile.application.query;

import com.cinx.user.profile.domain.Gender;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import com.cinx.user.profile.domain.PartnershipTerminationReasonType;
import com.cinx.user.profile.domain.User;

import java.time.LocalDateTime;

/** What the rest of the platform may see of a profile. */
public record UserView(
        String userId,
        String name,
        String email,
        Gender gender,
        Boolean isReceivePushNotification,
        Boolean isInstructorVerified,
        String avatarUrl,
        String phoneNumber,
        String bio,
        String cvUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastAccessAt,
        LocalDateTime instructorVerifiedAt,
        LocalDateTime partnershipTerminatedAt,
        PartnershipTerminationReasonType partnershipTerminationReasonType,
        String partnershipTerminationReasonDetail,
        Boolean isPartnershipTerminated,
        InstructorApplicationStatus instructorApplicationStatus) {

    public static UserView of(User user) {
        return new UserView(user.getUserId(), user.getName(), user.getEmail(), user.getGender(),
                user.getIsReceivePushNotification(), user.getIsInstructorVerified(), user.getAvatarUrl(),
                user.getPhoneNumber(), user.getBio(), user.getCvUrl(), user.getCreatedAt(), user.getUpdatedAt(),
                user.getLastAccessAt(), user.getInstructorVerifiedAt(), user.getPartnershipTerminatedAt(),
                user.getPartnershipTerminationReasonType(), user.getPartnershipTerminationReasonDetail(),
                user.getIsPartnershipTerminated(), user.getInstructorApplicationStatus());
    }
}
