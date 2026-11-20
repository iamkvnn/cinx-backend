package com.cinx.user.profile.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.application.AcceptedPolicy;
import com.cinx.user.policy.application.PolicyAcceptances;
import com.cinx.user.profile.application.command.CreateUserCommand;
import com.cinx.user.profile.application.command.InstructorApplicationCommand;
import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.OnboardProfileCommand;
import com.cinx.user.profile.application.command.TerminatePartnershipCommand;
import com.cinx.user.profile.application.command.UpdateProfileCommand;
import com.cinx.user.profile.application.query.InstructorLookup;
import com.cinx.user.profile.application.query.UserQueries;
import com.cinx.user.profile.application.query.UserView;
import com.cinx.user.profile.domain.AccountType;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import com.cinx.user.profile.domain.User;
import com.cinx.user.profile.domain.UserRepository;
import com.cinx.user.shared.application.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final UserQueries userQueries;
    private final PolicyAcceptances policyAcceptances;
    private final UserEvents userEvents;
    private final ObjectStorage objectStorage;
    private final Clock clock;
    private final ILearnerProfileService learnerProfiles;

    private User getOrThrowByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    private User getOrThrowByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User not found with userId: " + userId));
    }

    @Override
    @Transactional
    public UserView onboardProfile(
            String userId,
            String keycloakSubject,
            String email,
            AccountType accountType,
            OnboardProfileCommand command
    ) {
        return onboardProfile(userId, keycloakSubject, email, accountType, command, "legacy-" + userId);
    }

    @Override
    @Transactional
    public UserView onboardProfile(
            String userId,
            String keycloakSubject,
            String email,
            AccountType accountType,
            OnboardProfileCommand command,
            String idempotencyKey
    ) {
        validateIdempotencyKey(idempotencyKey);
        String fingerprint = requestFingerprint(command);
        Optional<User> existingProfile = userRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            User existing = existingProfile.get();
            if (idempotencyKey.equals(existing.getOnboardingIdempotencyKey())
                    && fingerprint.equals(existing.getOnboardingRequestFingerprint())) {
                return UserView.of(existing);
            }
            throw new AlreadyExistException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Profile already exists for userId: " + userId);
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new AlreadyExistException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Profile already exists for email: " + email);
        }
        List<AcceptedPolicy> acceptedPolicies = policyAcceptances.resolveAccepted(command.acceptedPolicyIds());
        User user = userRepository.save(User.builder()
                .userId(userId)
                .email(email)
                .name(command.name())
                .isInstructorVerified(false)
                .gender(command.gender())
                .phoneNumber(command.phoneNumber())
                .bio(command.bio())
                .cvFileKey(command.cvFileKey())
                .cvUrl(command.cvFileKey() == null ? null : objectStorage.publicUrl(command.cvFileKey()))
                .onboardingIdempotencyKey(idempotencyKey)
                .onboardingRequestFingerprint(fingerprint)
                .build());
        userEvents.publishProfileOnboarded(
                user,
                keycloakSubject);
        if (accountType == AccountType.INSTRUCTOR) {
            userEvents.publishInstructorApplicationSubmitted(user);
        }
        if (!acceptedPolicies.isEmpty()) {
            policyAcceptances.record(user.getUserId(), acceptedPolicies, now());
        }
        List<String> preferredCategoryIds = distinctIds(command.preferredCategoryIds());
        if (!preferredCategoryIds.isEmpty()) {
            userEvents.publishPreferredCategoriesUpdated(user.getUserId(), preferredCategoryIds);
        }
        learnerProfiles.recordOnboarding(user.getUserId(), command.learningProfile(), preferredCategoryIds);
        return UserView.of(user);
    }

    private List<String> distinctIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST,
                    "A valid Idempotency-Key header is required");
        }
    }

    private String requestFingerprint(OnboardProfileCommand command) {
        if (command == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Onboarding request is required");
        }
        String payload = String.join("\u001f",
                valueOrEmpty(command.name()),
                valueOrEmpty(command.gender() == null ? null : command.gender().name()),
                valueOrEmpty(command.phoneNumber()),
                valueOrEmpty(command.bio()),
                valueOrEmpty(command.cvFileKey()));
        // The id lists join the fingerprint only when present, so a request without them keeps its old fingerprint
        List<String> categoryIds = distinctIds(command.preferredCategoryIds()).stream().sorted().toList();
        List<String> policyIds = distinctIds(command.acceptedPolicyIds()).stream().sorted().toList();
        if (!categoryIds.isEmpty() || !policyIds.isEmpty()) {
            payload += "\u001f" + String.join(",", categoryIds) + "\u001f" + String.join(",", policyIds);
        }
        LearnerProfileAnswers answers = command.learningProfile();
        if (answers != null) {
            payload += "\u001fprofile:" + String.join(",",
                    valueOrEmpty(answers.goal() == null ? null : answers.goal().name()),
                    valueOrEmpty(answers.level() == null ? null : answers.level().name()),
                    valueOrEmpty(answers.minutesPerDay() == null ? null : answers.minutesPerDay().toString()),
                    valueOrEmpty(answers.contentLanguage() == null ? null : answers.contentLanguage().name()));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public PageResult<UserView> findAll(int page, int size, String query, Boolean isInstructorVerified,
                                        InstructorApplicationStatus applicationStatus, String sort) {
        return userQueries.findAll(query, isInstructorVerified, applicationStatus, new PageQuery(page, size, sort));
    }

    @Override
    public UserView findByEmail(String email) {
        return UserView.of(getOrThrowByEmail(email));
    }

    @Override
    public UserView findByUserId(String userId) {
        return UserView.of(getOrThrowByUserId(userId));
    }

    @Override
    public Boolean checkInstructorVerified(String id) {
        return getOrThrowByUserId(id).isActiveInstructor();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLookup lookupVerifiedInstructor(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        List<User> matches = normalized.isEmpty() ? List.of() : userRepository.findAllByEmailIgnoreCase(normalized);
        if (matches.size() > 1) {
            // user.email is not unique in the schema; refuse to guess which account is meant.
            log.warn("Instructor lookup matched {} users for one email", matches.size());
        }
        return Optional.of(matches)
                .filter(found -> found.size() == 1)
                .map(found -> found.get(0))
                .filter(User::isActiveInstructor)
                .map(user -> new InstructorLookup(user.getUserId(), user.getName(), user.getAvatarUrl()))
                .orElseThrow(() -> new NotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, "No verified instructor uses this email"));
    }

    @Override
    @Transactional
    public UserView createUser(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "User already exists with email: " + command.email());
        }
        User user = User
                .builder()
                .userId(command.userId())
                .email(command.email())
                .name(command.name())
                .isInstructorVerified(false)
                .gender(command.gender())
                .phoneNumber(command.phoneNumber())
                .bio(command.bio())
                .cvFileKey(command.cvFileKey())
                .cvUrl(command.cvFileKey() != null ? objectStorage.publicUrl(command.cvFileKey()) : null)
                .build();
        if (command.accountType() == AccountType.INSTRUCTOR) {
            user.startInstructorApplication(now());
        }
        user = userRepository.save(user);
        // Only notify admins for new instructor registrations
        if (command.accountType() == AccountType.INSTRUCTOR) {
            userEvents.publishInstructorApplicationSubmitted(user);
        }
        return UserView.of(user);
    }

    @Override
    @Transactional
    public void verifyInstructor(String id) {
        User user = getOrThrowByUserId(id);
        user.approveAsInstructor(now());
        userRepository.save(user);
        userEvents.sendInstructorVerifiedEmail(user);
    }

    @Override
    @Transactional
    public void rejectInstructor(String id, String reason) {
        User user = getOrThrowByUserId(id);
        user.rejectInstructorApplication(reason, now());
        userRepository.save(user);
        userEvents.sendInstructorRejectedEmail(user);
    }

    @Override
    @Transactional
    public UserView applyInstructor(String userId, InstructorApplicationCommand command) {
        User user = getOrThrowByUserId(userId);
        if (user.hasActiveInstructorApplication()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "An instructor application is already active");
        }
        if (command != null && command.cvFileKey() != null && !command.cvFileKey().isBlank()
                && !command.cvFileKey().equals(user.getCvFileKey())) {
            String cvFileKey = command.cvFileKey().trim();
            user.setCvFileKey(cvFileKey);
            user.setCvUrl(objectStorage.publicUrl(cvFileKey));
        }
        if (user.getCvFileKey() == null || user.getCvFileKey().isBlank()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A CV is required for instructor application");
        }
        user.resubmitInstructorApplication(now());
        userRepository.save(user);
        userEvents.publishInstructorApplicationSubmitted(user);
        return UserView.of(user);
    }

    @Override
    @Transactional
    public void terminatePartnership(String id, TerminatePartnershipCommand command) {
        User user = getOrThrowByUserId(id);
        user.terminatePartnership(command.reasonType(), command.reasonDetail(), now());
        userRepository.save(user);
        userEvents.sendPartnershipTerminatedEmail(user);
    }

    @Override
    @Transactional
    public UserView updateProfile(String id, UpdateProfileCommand command) {
        User existingUser = getOrThrowByUserId(id);

        if (command.avatarFileKey() != null && !command.avatarFileKey().equals(existingUser.getAvatarFileKey())) {
            if (existingUser.getAvatarFileKey() != null) {
                objectStorage.deleteObject(existingUser.getAvatarFileKey());
            }
            existingUser.setAvatarUrl(objectStorage.publicUrl(command.avatarFileKey()));
        }

        if (command.cvFileKey() != null && !command.cvFileKey().equals(existingUser.getCvFileKey())) {
            if (existingUser.getCvUrl() != null) {
                objectStorage.deleteObject(existingUser.getCvFileKey());
            }
            existingUser.setCvUrl(objectStorage.publicUrl(command.cvFileKey()));
        }
        existingUser.applyProfileChanges(command.name(), command.gender(), command.phoneNumber(), command.bio(),
                command.isReceivePushNotification(), command.avatarFileKey(), command.cvFileKey());
        return UserView.of(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void updateLastAccess(String userId) {
        User user = getOrThrowByUserId(userId);
        user.setLastAccessAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<UserView> findByIds(List<String> ids) {
        return userRepository.findAllByUserIdIn(ids).stream().map(UserView::of).toList();
    }

    @Override
    @Transactional
    public void updatePreferredCategories(String userId, List<String> categoryIds) {
        getOrThrowByUserId(userId);
        List<String> normalizedCategoryIds = categoryIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(categoryId -> !categoryId.isBlank())
                .distinct()
                .toList();
        userEvents.publishPreferredCategoriesUpdated(userId, normalizedCategoryIds);
        learnerProfiles.replaceCategories(userId, normalizedCategoryIds);
    }

    @Override
    @Transactional
    public UserView updateEffects(String userId, boolean enabled) {
        User user = getOrThrowByUserId(userId);
        user.setEffectsEnabled(enabled);
        return UserView.of(userRepository.save(user));
    }

    @Override
    public long countTotalUsers() {
        return userQueries.countTotalUsers();
    }

    @Override
    public long countUsersBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return userQueries.countUsersBetween(start, end);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock == null ? Clock.systemUTC() : clock);
    }
}
