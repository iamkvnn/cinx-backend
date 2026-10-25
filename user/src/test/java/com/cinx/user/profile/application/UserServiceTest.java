package com.cinx.user.profile.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.policy.application.AcceptedPolicy;
import com.cinx.user.policy.application.PolicyAcceptances;
import com.cinx.user.policy.domain.PolicyType;
import com.cinx.user.profile.application.command.CreateUserCommand;
import com.cinx.user.profile.application.command.InstructorApplicationCommand;
import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.OnboardProfileCommand;
import com.cinx.user.profile.application.command.TerminatePartnershipCommand;
import com.cinx.user.profile.application.command.UpdateProfileCommand;
import com.cinx.user.profile.application.query.UserQueries;
import com.cinx.user.profile.application.query.UserView;
import com.cinx.user.profile.domain.AccountType;
import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.Gender;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearningGoal;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import com.cinx.user.profile.domain.PartnershipTerminationReasonType;
import com.cinx.user.profile.domain.User;
import com.cinx.user.profile.domain.UserRepository;
import com.cinx.user.shared.application.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Mock UserRepository userRepository;
    @Mock UserQueries userQueries;
    @Mock PolicyAcceptances policyAcceptances;
    @Mock UserEvents userEvents;
    @Mock ObjectStorage objectStorage;
    @Mock ILearnerProfileService learnerProfiles;
    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userQueries, policyAcceptances, userEvents, objectStorage,
                Clock.fixed(Instant.parse("2026-08-01T10:00:00Z"), ZoneOffset.UTC), learnerProfiles);
    }

    // ---- creating and reading

    @Test
    void createUserPersistsProfileFieldsAndBuildsTheCvAddressFromTheStorage() {
        when(objectStorage.publicUrl("cv.pdf")).thenReturn("https://cdn.example.com/cv.pdf");
        when(userRepository.existsByEmail("tranthib@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserView view = userService.createUser(new CreateUserCommand("user-1", "Tran Thi B", "tranthib@example.com",
                AccountType.STUDENT, Gender.FEMALE, "0987654321", "Bio", "cv.pdf"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(saved.getBio()).isEqualTo("Bio");
        assertThat(saved.getCvFileKey()).isEqualTo("cv.pdf");
        assertThat(saved.getCvUrl()).isEqualTo("https://cdn.example.com/cv.pdf");
        assertThat(saved.getIsInstructorVerified()).isFalse();
        assertThat(saved.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.NONE);
        assertThat(view.userId()).isEqualTo("user-1");
        verify(userEvents, never()).publishInstructorApplicationSubmitted(any(User.class));
    }

    @Test
    void createUserWithoutACvHasNoCvAddress() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(new CreateUserCommand("user-1", "A", "a@example.com", AccountType.STUDENT, null, null, null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getCvUrl()).isNull();
        verify(objectStorage, never()).publicUrl(any());
    }

    @Test
    void createInstructorStartsAnApplicationAndPublishesIt() {
        when(userRepository.existsByEmail("instructor@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(new CreateUserCommand("instructor-1", "Instructor", "instructor@example.com",
                AccountType.INSTRUCTOR, Gender.MALE, null, null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userEvents).publishInstructorApplicationSubmitted(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("instructor-1");
        assertThat(captor.getValue().getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.PENDING);
        assertThat(captor.getValue().getInstructorApplicationSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void createUserWithAnEmailThatIsTakenIsAConflict() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserCommand("user-1", "A", "taken@example.com",
                AccountType.STUDENT, null, null, null, null))).isInstanceOf(AlreadyExistException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUserIdOfAnUnknownUserIsNotFound() {
        when(userRepository.findByUserId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUserId("ghost")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllPassesTheFiltersAndThePageToTheQueryPort() {
        PageResult<UserView> page = new PageResult<>(List.of(), 2, 5, 0);
        when(userQueries.findAll("tran", true, InstructorApplicationStatus.PENDING,
                new PageQuery(2, 5, "{\"name\":\"ASC\"}"))).thenReturn(page);

        assertThat(userService.findAll(2, 5, "tran", true, InstructorApplicationStatus.PENDING,
                "{\"name\":\"ASC\"}")).isSameAs(page);
    }

    @Test
    void metricsComeFromTheQueryPort() {
        when(userQueries.countTotalUsers()).thenReturn(42L);
        when(userQueries.countUsersBetween(NOW.minusDays(1), NOW)).thenReturn(3L);

        assertThat(userService.countTotalUsers()).isEqualTo(42L);
        assertThat(userService.countUsersBetween(NOW.minusDays(1), NOW)).isEqualTo(3L);
    }

    // ---- instructor workflow

    @Test
    void verifyInstructorApprovesTheApplicationAndSendsTheEmail() {
        User user = User.builder().userId("user-1").email("instructor@example.com").name("Instructor")
                .instructorApplicationStatus(InstructorApplicationStatus.PENDING).build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));

        userService.verifyInstructor("user-1");

        assertThat(user.getIsInstructorVerified()).isTrue();
        assertThat(user.getInstructorVerifiedAt()).isEqualTo(NOW);
        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.APPROVED);
        assertThat(user.getInstructorApplicationReviewedAt()).isEqualTo(NOW);
        verify(userRepository).save(user);
        verify(userEvents).sendInstructorVerifiedEmail(user);
    }

    @Test
    void rejectInstructorRecordsTheTrimmedReasonAndSendsTheEmail() {
        User user = User.builder().userId("user-1").isInstructorVerified(true)
                .instructorApplicationStatus(InstructorApplicationStatus.PENDING).build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));

        userService.rejectInstructor("user-1", "  Incomplete CV  ");

        assertThat(user.getIsInstructorVerified()).isFalse();
        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.REJECTED);
        assertThat(user.getInstructorApplicationRejectionReason()).isEqualTo("Incomplete CV");
        verify(userRepository).save(user);
        verify(userEvents).sendInstructorRejectedEmail(user);
    }

    @Test
    void terminatePartnershipStoresTerminationReasonAndTime() {
        User user = User.builder().userId("instructor-1").isInstructorVerified(true).isPartnershipTerminated(false).build();
        when(userRepository.findByUserId("instructor-1")).thenReturn(Optional.of(user));

        userService.terminatePartnership("instructor-1", new TerminatePartnershipCommand(
                PartnershipTerminationReasonType.POLICY_VIOLATION, " Repeated violations "));

        assertThat(user.getIsInstructorVerified()).isFalse();
        assertThat(user.getIsPartnershipTerminated()).isTrue();
        assertThat(user.getPartnershipTerminatedAt()).isEqualTo(NOW);
        assertThat(user.getPartnershipTerminationReasonType()).isEqualTo(PartnershipTerminationReasonType.POLICY_VIOLATION);
        assertThat(user.getPartnershipTerminationReasonDetail()).isEqualTo("Repeated violations");
        verify(userRepository).save(user);
        verify(userEvents).sendPartnershipTerminatedEmail(user);
    }

    @Test
    void applyingAsInstructorWithAnExistingCvStartsAFreshApplication() {
        User user = User.builder().userId("user-1").cvFileKey("cv.pdf").cvUrl("https://cdn/cv.pdf")
                .instructorApplicationStatus(InstructorApplicationStatus.REJECTED).instructorApplicationRejectionReason("old")
                .instructorApplicationReviewedAt(NOW.minusDays(3)).isPartnershipTerminated(true).build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.applyInstructor("user-1", null);

        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.PENDING);
        assertThat(user.getInstructorApplicationSubmittedAt()).isEqualTo(NOW);
        assertThat(user.getInstructorApplicationReviewedAt()).isNull();
        assertThat(user.getInstructorApplicationRejectionReason()).isNull();
        assertThat(user.getIsPartnershipTerminated()).isFalse();
        verify(userEvents).publishInstructorApplicationSubmitted(user);
    }

    @Test
    void applyingAsInstructorWithANewCvReplacesTheCvAndBuildsItsAddress() {
        User user = User.builder().userId("user-1").cvFileKey("old.pdf").cvUrl("https://cdn/old.pdf").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(objectStorage.publicUrl("new.pdf")).thenReturn("https://cdn/new.pdf");
        when(userRepository.save(user)).thenReturn(user);

        userService.applyInstructor("user-1", new InstructorApplicationCommand("  new.pdf  "));

        assertThat(user.getCvFileKey()).isEqualTo("new.pdf");
        assertThat(user.getCvUrl()).isEqualTo("https://cdn/new.pdf");
    }

    @Test
    void applyingAsInstructorWithoutAnyCvIsRefused() {
        User user = User.builder().userId("user-1").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.applyInstructor("user-1", new InstructorApplicationCommand("  ")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CV is required");
        verify(userRepository, never()).save(any());
        verify(userEvents, never()).publishInstructorApplicationSubmitted(any());
    }

    @Test
    void applyingAsInstructorWhileAnApplicationIsPendingOrVerifiedIsRefused() {
        User pending = User.builder().userId("pending").cvFileKey("cv").instructorApplicationStatus(InstructorApplicationStatus.PENDING).build();
        User verified = User.builder().userId("verified").cvFileKey("cv").isInstructorVerified(true).build();
        when(userRepository.findByUserId("pending")).thenReturn(Optional.of(pending));
        when(userRepository.findByUserId("verified")).thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> userService.applyInstructor("pending", null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.applyInstructor("verified", null)).isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void anInstructorWhosePartnershipWasTerminatedIsNotVerifiedForOthers() {
        when(userRepository.findByUserId("active")).thenReturn(Optional.of(User.builder().userId("active").isInstructorVerified(true).build()));
        when(userRepository.findByUserId("cut")).thenReturn(Optional.of(
                User.builder().userId("cut").isInstructorVerified(true).isPartnershipTerminated(true).build()));
        when(userRepository.findByUserId("learner")).thenReturn(Optional.of(User.builder().userId("learner").isInstructorVerified(false).build()));

        assertThat(userService.checkInstructorVerified("active")).isTrue();
        assertThat(userService.checkInstructorVerified("cut")).isFalse();
        assertThat(userService.checkInstructorVerified("learner")).isFalse();
    }

    @Test
    void lookupFindsTheOneVerifiedInstructorWhateverTheCaseAndSpacesOfTheEmail() {
        User instructor = User.builder().userId("instructor-1").name("Instructor").avatarUrl("https://cdn/a.png")
                .email("Instructor@Example.com").isInstructorVerified(true).build();
        when(userRepository.findAllByEmailIgnoreCase("instructor@example.com")).thenReturn(List.of(instructor));

        var lookup = userService.lookupVerifiedInstructor("  Instructor@Example.COM ");

        assertThat(lookup.id()).isEqualTo("instructor-1");
        assertThat(lookup.name()).isEqualTo("Instructor");
        assertThat(lookup.avatarUrl()).isEqualTo("https://cdn/a.png");
    }

    @Test
    void lookupRefusesToGuessWhenSeveralAccountsShareTheEmailAndWhenTheMatchIsNotAnActiveInstructor() {
        User a = User.builder().userId("a").isInstructorVerified(true).build();
        User b = User.builder().userId("b").isInstructorVerified(true).build();
        User learner = User.builder().userId("c").isInstructorVerified(false).build();
        User cut = User.builder().userId("d").isInstructorVerified(true).isPartnershipTerminated(true).build();
        when(userRepository.findAllByEmailIgnoreCase("many@example.com")).thenReturn(List.of(a, b));
        when(userRepository.findAllByEmailIgnoreCase("learner@example.com")).thenReturn(List.of(learner));
        when(userRepository.findAllByEmailIgnoreCase("cut@example.com")).thenReturn(List.of(cut));
        when(userRepository.findAllByEmailIgnoreCase("none@example.com")).thenReturn(List.of());

        for (String email : List.of("many@example.com", "learner@example.com", "cut@example.com", "none@example.com")) {
            assertThatThrownBy(() -> userService.lookupVerifiedInstructor(email)).as(email).isInstanceOf(NotFoundException.class);
        }
        assertThatThrownBy(() -> userService.lookupVerifiedInstructor("   ")).isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).findAllByEmailIgnoreCase("");
    }

    // ---- profile changes

    @Test
    void updateLastAccessStoresCurrentTimestamp() {
        User user = User.builder().userId("user-1").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));

        userService.updateLastAccess("user-1");

        assertThat(user.getLastAccessAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updatingTheProfileChangesOnlyTheFieldsSent() {
        User user = User.builder().userId("user-1").name("Old Name").gender(Gender.MALE).phoneNumber("111").bio("old bio")
                .isReceivePushNotification(false).build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateProfile("user-1", new UpdateProfileCommand("New Name", null, null, "new bio", true, null, null));

        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getBio()).isEqualTo("new bio");
        assertThat(user.getIsReceivePushNotification()).isTrue();
        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getPhoneNumber()).isEqualTo("111");
        verify(objectStorage, never()).deleteObject(any());
    }

    @Test
    void aNewAvatarReplacesTheOldFileAndTheAddress() {
        User user = User.builder().userId("user-1").avatarFileKey("old.png").avatarUrl("https://cdn/old.png").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(objectStorage.publicUrl("new.png")).thenReturn("https://cdn/new.png");
        when(userRepository.save(user)).thenReturn(user);

        userService.updateProfile("user-1", new UpdateProfileCommand("Name", null, null, null, null, "new.png", null));

        verify(objectStorage).deleteObject("old.png");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn/new.png");
        assertThat(user.getAvatarFileKey()).isEqualTo("new.png");
    }

    @Test
    void theSameAvatarKeyAgainTouchesNoFile() {
        User user = User.builder().userId("user-1").avatarFileKey("same.png").avatarUrl("https://cdn/same.png").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateProfile("user-1", new UpdateProfileCommand("Name", null, null, null, null, "same.png", null));

        verify(objectStorage, never()).deleteObject(any());
        verify(objectStorage, never()).publicUrl(any());
    }

    @Test
    void aNewCvDeletesTheOldOneOnlyWhenThereWasAnAddress() {
        User withCv = User.builder().userId("a").cvFileKey("old.pdf").cvUrl("https://cdn/old.pdf").build();
        User withoutCv = User.builder().userId("b").build();
        when(userRepository.findByUserId("a")).thenReturn(Optional.of(withCv));
        when(userRepository.findByUserId("b")).thenReturn(Optional.of(withoutCv));
        when(objectStorage.publicUrl("new.pdf")).thenReturn("https://cdn/new.pdf");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateProfile("a", new UpdateProfileCommand("A", null, null, null, null, null, "new.pdf"));
        userService.updateProfile("b", new UpdateProfileCommand("B", null, null, null, null, null, "new.pdf"));

        verify(objectStorage, times(1)).deleteObject("old.pdf");
        assertThat(withCv.getCvUrl()).isEqualTo("https://cdn/new.pdf");
        assertThat(withoutCv.getCvUrl()).isEqualTo("https://cdn/new.pdf");
    }

    @Test
    void preferredCategoriesAreCleanedBeforeTheyAreSent() {
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(User.builder().userId("user-1").build()));

        userService.updatePreferredCategories("user-1", java.util.Arrays.asList(" c1 ", "c1", "", null, "c2"));

        verify(userEvents).publishPreferredCategoriesUpdated("user-1", List.of("c1", "c2"));
    }

    @Test
    void preferredCategoriesOfAnUnknownUserAreRefused() {
        when(userRepository.findByUserId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePreferredCategories("ghost", List.of("c1"))).isInstanceOf(NotFoundException.class);
        verify(userEvents, never()).publishPreferredCategoriesUpdated(any(), any());
    }

    // ---- onboarding: idempotency, agreement, topics

    @Test
    void onboardingRetriesWithTheSameKeyAndFingerprintReturnTheExistingProfile() {
        OnboardProfileCommand command = new OnboardProfileCommand("Tran Thi B", Gender.FEMALE, "0987654321", "Bio", "cv.pdf", null, null, null);
        when(objectStorage.publicUrl("cv.pdf")).thenReturn("https://cdn/cv.pdf");
        User stored = User.builder().userId("user-1").email("user@example.com").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.empty(), Optional.of(stored));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User candidate = invocation.getArgument(0);
            stored.setOnboardingIdempotencyKey(candidate.getOnboardingIdempotencyKey());
            stored.setOnboardingRequestFingerprint(candidate.getOnboardingRequestFingerprint());
            return candidate;
        });

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, command, "onboarding-key");
        UserView retry = userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, command, "onboarding-key");

        assertThat(stored.getOnboardingIdempotencyKey()).isEqualTo("onboarding-key");
        assertThat(stored.getOnboardingRequestFingerprint()).isNotBlank();
        assertThat(retry.userId()).isEqualTo("user-1");
        verify(userEvents, times(1)).publishProfileOnboarded(any(User.class), eq("subject-1"));
    }

    @Test
    void onboardingWithAnotherKeyOrAnotherBodyForAnExistingProfileIsAConflict() {
        User existing = User.builder().userId("user-1").onboardingIdempotencyKey("key-1").onboardingRequestFingerprint("not-this-one").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(null, null), "key-1")).isInstanceOf(AlreadyExistException.class);
        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(null, null), "key-2")).isInstanceOf(AlreadyExistException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardingWithAnEmailAnotherProfileUsesIsAConflict() {
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "taken@example.com", AccountType.STUDENT,
                onboarding(null, null), "key-1")).isInstanceOf(AlreadyExistException.class).hasMessageContaining("email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardingNeedsAValidIdempotencyKey() {
        for (String key : java.util.Arrays.asList(null, "", "  ", "k".repeat(256))) {
            assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                    onboarding(null, null), key)).as("key %s", key == null ? "null" : key.length() + " chars").isInstanceOf(BadRequestException.class);
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardingWithoutABodyIsRefused() {
        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, null, "key-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("required");
    }

    @Test
    void onboardingRecordsTheAgreementOfTheAcceptedPoliciesForTheNewUser() {
        List<AcceptedPolicy> accepted = List.of(new AcceptedPolicy("terms-1", 3, PolicyType.TERMS), new AcceptedPolicy("privacy-1", 2, PolicyType.PRIVACY));
        when(policyAcceptances.resolveAccepted(List.of("terms-1", "privacy-1"))).thenReturn(accepted);
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(null, List.of("terms-1", "privacy-1")), "key-1");

        verify(policyAcceptances).record("user-1", accepted, NOW);
    }

    @Test
    void anAgreementThatIsRefusedStopsTheOnboardingBeforeAnythingIsStored() {
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(policyAcceptances.resolveAccepted(any())).thenThrow(new BadRequestException("Agreement to the published PRIVACY policy is required"));

        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(null, List.of("terms-1")), "key-1")).isInstanceOf(BadRequestException.class).hasMessageContaining("PRIVACY");
        verify(userRepository, never()).save(any(User.class));
        verify(policyAcceptances, never()).record(any(), any(), any());
        verify(userEvents, never()).publishProfileOnboarded(any(), any());
    }

    @Test
    void onboardingRecordsNothingWhenNoPolicyWasAccepted() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, onboarding(null, null), "key-1");

        verify(userRepository).save(any(User.class));
        verify(policyAcceptances, never()).record(any(), any(), any());
    }

    @Test
    void onboardingAnInstructorAccountAlsoPublishesTheApplication() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.INSTRUCTOR, onboarding(null, null), "key-1");

        verify(userEvents).publishInstructorApplicationSubmitted(any(User.class));
    }

    @Test
    void onboardingPublishesThePickedTopicsWithoutBlanksOrDuplicates() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(List.of(" c1 ", "c1", "", "c2"), null), "key-1");

        verify(userEvents).publishPreferredCategoriesUpdated("user-1", List.of("c1", "c2"));
    }

    @Test
    void onboardingPublishesNoPreferenceEventWhenNoTopicWasPicked() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, onboarding(List.of(), null), "key-1");

        verify(userEvents, never()).publishPreferredCategoriesUpdated(any(), any());
    }

    @Test
    void onboardingKeepsTheFingerprintOfTheFiveOriginalFieldsWhenNoTopicOrPolicyIsSent() throws Exception {
        when(objectStorage.publicUrl("cv.pdf")).thenReturn("https://cdn/cv.pdf");
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                new OnboardProfileCommand("Tran Thi B", Gender.FEMALE, "0987654321", "Bio", "cv.pdf", null, null, null), "key-1");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        // the fingerprint a request made before the two new fields existed would have had: SHA-256 of the five fields joined by U+001F
        String legacy = String.join("\u001f", "Tran Thi B", "FEMALE", "0987654321", "Bio", "cv.pdf");
        String expected = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(legacy.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(saved.getValue().getOnboardingRequestFingerprint()).isEqualTo(expected);
    }

    @Test
    void theTopicsAndPoliciesJoinTheFingerprintInSortedOrderSoTheirOrderDoesNotMatter() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(List.of("c2", "c1"), List.of("p2", "p1")), "key-1");
        userService.onboardProfile("user-2", "subject-1", "other@example.com", AccountType.STUDENT,
                onboarding(List.of("c1", "c2", "c1"), List.of("p1", "p2")), "key-2");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getOnboardingRequestFingerprint())
                .isEqualTo(saved.getAllValues().get(1).getOnboardingRequestFingerprint());
    }

    @Test
    void onboardingRetryWithTheSameKeyButOtherTopicsIsAConflict() {
        User existing = User.builder().userId("user-1").email("user@example.com").build();
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.empty(), Optional.of(existing), Optional.of(existing));
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User candidate = invocation.getArgument(0);
            existing.setOnboardingIdempotencyKey(candidate.getOnboardingIdempotencyKey());
            existing.setOnboardingRequestFingerprint(candidate.getOnboardingRequestFingerprint());
            return candidate;
        });

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, onboarding(List.of("c1"), null), "key-1");

        // the same body again is the same answer; another set of topics under the same key is not
        assertThat(userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, onboarding(List.of("c1"), null), "key-1").userId())
                .isEqualTo("user-1");
        assertThatThrownBy(() -> userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                onboarding(List.of("c2"), null), "key-1")).isInstanceOf(AlreadyExistException.class);
        verify(userEvents, times(1)).publishPreferredCategoriesUpdated(any(), any());
    }

    private void stubFreshProfile() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onboardingHandsTheProfileStepToTheLearnerProfile() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();
        LearnerProfileAnswers answers = new LearnerProfileAnswers(LearningGoal.SKILL_UP, LearnerLevel.BASIC, 30, ContentLanguage.VI);

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT,
                new OnboardProfileCommand("Tran Thi B", Gender.FEMALE, "0987654321", "Bio", null, List.of("c1"), null, answers), "key-1");

        verify(learnerProfiles).recordOnboarding("user-1", answers, List.of("c1"));
    }

    @Test
    void theProfileStepJoinsTheFingerprintOnlyWhenSent() {
        when(policyAcceptances.resolveAccepted(any())).thenReturn(List.of());
        stubFreshProfile();

        userService.onboardProfile("user-1", "subject-1", "user@example.com", AccountType.STUDENT, onboarding(List.of("c1"), null), "key-1");
        userService.onboardProfile("user-2", "subject-1", "other@example.com", AccountType.STUDENT,
                new OnboardProfileCommand("Tran Thi B", Gender.FEMALE, "0987654321", "Bio", null, List.of("c1"), null,
                        new LearnerProfileAnswers(null, null, null, null)), "key-2");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getOnboardingRequestFingerprint())
                .isNotEqualTo(saved.getAllValues().get(1).getOnboardingRequestFingerprint());
    }

    @Test
    void thePreferredCategoriesEndpointAlsoKeepsTheTopicsInTheProfile() {
        when(userRepository.findByUserId("user-1")).thenReturn(Optional.of(User.builder().userId("user-1").build()));

        userService.updatePreferredCategories("user-1", List.of(" c1 ", "c1", "c2"));

        verify(userEvents).publishPreferredCategoriesUpdated("user-1", List.of("c1", "c2"));
        verify(learnerProfiles).replaceCategories("user-1", List.of("c1", "c2"));
    }

    private static OnboardProfileCommand onboarding(List<String> topics, List<String> policies) {
        return new OnboardProfileCommand("Tran Thi B", Gender.FEMALE, "0987654321", "Bio", null, topics, policies, null);
    }
}
