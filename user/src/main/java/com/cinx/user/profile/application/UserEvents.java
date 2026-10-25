package com.cinx.user.profile.application;

import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.User;

import java.util.List;

/** Port: tells the rest of the platform what happened to a profile. Delivery is the outbox's job. */
public interface UserEvents {
    void publishProfileOnboarded(User user, String keycloakSubject);

    void publishInstructorApplicationSubmitted(User user);

    void publishPreferredCategoriesUpdated(String userId, List<String> categoryIds);

    /** user.learning-profile.updated v1: the whole profile, so a consumer keeps the newest snapshot. */
    void publishLearningProfileUpdated(LearnerProfile profile);

    /** The instructor application was approved. */
    void sendInstructorVerifiedEmail(User user);

    void sendInstructorRejectedEmail(User user);

    void sendPartnershipTerminatedEmail(User user);
}
