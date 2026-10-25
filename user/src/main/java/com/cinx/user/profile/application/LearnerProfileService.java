package com.cinx.user.profile.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.SaveLearnerProfileCommand;
import com.cinx.user.profile.application.query.LearnerProfileView;
import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.LearnerProfileRepository;
import com.cinx.user.profile.domain.LearnerProfileStatus;
import com.cinx.user.profile.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LearnerProfileService implements ILearnerProfileService {
    private final LearnerProfileRepository profiles;
    private final UserRepository users;
    private final UserEvents userEvents;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public LearnerProfileView get(String userId) {
        return LearnerProfileView.of(profiles.findByUserId(userId).orElseGet(() -> LearnerProfile.empty(userId)));
    }

    @Override
    @Transactional
    public LearnerProfileView save(String userId, SaveLearnerProfileCommand command) {
        requireUser(userId);
        validateMinutes(command.minutesPerDay());
        List<String> categories = distinct(command.categoryIds());
        if (categories.size() > LearnerProfile.MAX_CATEGORIES) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Pick at most " + LearnerProfile.MAX_CATEGORIES + " topics");
        }
        LearnerProfile before = profiles.findByUserId(userId).orElseGet(() -> LearnerProfile.empty(userId));
        LearnerProfile saved = profiles.save(before.toBuilder()
                .goal(command.goal())
                .level(command.level())
                .minutesPerDay(command.minutesPerDay())
                .contentLanguage(command.contentLanguage())
                .categoryIds(categories)
                .personalizationConsent(command.personalizationConsent() == null
                        ? before.isPersonalizationConsent() : command.personalizationConsent())
                .usageAnalyticsConsent(command.usageAnalyticsConsent() == null
                        ? before.isUsageAnalyticsConsent() : command.usageAnalyticsConsent())
                // a privacy switch saves the stored, possibly empty, answers; only answers or topics complete a profile
                .status(answered(command, categories) ? LearnerProfileStatus.COMPLETED : before.getStatus())
                .updatedAt(now())
                .build());
        userEvents.publishLearningProfileUpdated(saved);
        // topics the learner never re-picked are still known to recommendation from before this profile existed
        if (!new HashSet<>(categories).equals(new HashSet<>(before.getCategoryIds()))) {
            userEvents.publishPreferredCategoriesUpdated(userId, categories);
        }
        return LearnerProfileView.of(saved);
    }

    @Override
    @Transactional
    public LearnerProfileView skip(String userId) {
        requireUser(userId);
        LearnerProfile current = profiles.findByUserId(userId).orElseGet(() -> LearnerProfile.empty(userId));
        if (current.getStatus() != LearnerProfileStatus.NONE) {
            return LearnerProfileView.of(current);
        }
        return LearnerProfileView.of(profiles.save(current.toBuilder()
                .status(LearnerProfileStatus.SKIPPED)
                .updatedAt(now())
                .build()));
    }

    @Override
    @Transactional
    public void recordOnboarding(String userId, LearnerProfileAnswers answers, List<String> categoryIds) {
        if (answers == null && categoryIds.isEmpty()) {
            return;
        }
        LearnerProfileStatus status = answers == null ? LearnerProfileStatus.NONE
                : answers.answered() ? LearnerProfileStatus.COMPLETED : LearnerProfileStatus.SKIPPED;
        if (answers != null) {
            validateMinutes(answers.minutesPerDay());
        }
        LearnerProfile saved = profiles.save(LearnerProfile.empty(userId).toBuilder()
                .goal(answers == null ? null : answers.goal())
                .level(answers == null ? null : answers.level())
                .minutesPerDay(answers == null ? null : answers.minutesPerDay())
                .contentLanguage(answers == null ? null : answers.contentLanguage())
                .categoryIds(categoryIds)
                .status(status)
                .updatedAt(now())
                .build());
        userEvents.publishLearningProfileUpdated(saved);
    }

    @Override
    @Transactional
    public void replaceCategories(String userId, List<String> categoryIds) {
        LearnerProfile current = profiles.findByUserId(userId).orElseGet(() -> LearnerProfile.empty(userId));
        LearnerProfile saved = profiles.save(current.toBuilder().categoryIds(categoryIds).updatedAt(now()).build());
        userEvents.publishLearningProfileUpdated(saved);
    }

    private static boolean answered(SaveLearnerProfileCommand command, List<String> categories) {
        return command.goal() != null || command.level() != null || command.minutesPerDay() != null
                || command.contentLanguage() != null || !categories.isEmpty();
    }

    private void requireUser(String userId) {
        if (users.findByUserId(userId).isEmpty()) {
            throw new NotFoundException("User not found with userId: " + userId);
        }
    }

    private static void validateMinutes(Integer minutesPerDay) {
        if (minutesPerDay != null && !LearnerProfile.MINUTES_PER_DAY.contains(minutesPerDay)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Minutes per day must be one of 15, 30, 45, 60, 90 or 120");
        }
    }

    private static List<String> distinct(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isBlank()).distinct().toList();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
