package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.Gender;

import java.util.List;

/**
 * The first profile of a signed-in user, with the topics they like and the policies they agreed to.
 *
 * @param learningProfile the "Mục tiêu học" step; null from a client older than it, empty when skipped
 */
public record OnboardProfileCommand(
        String name,
        Gender gender,
        String phoneNumber,
        String bio,
        String cvFileKey,
        List<String> preferredCategoryIds,
        List<String> acceptedPolicyIds,
        LearnerProfileAnswers learningProfile) {
}
