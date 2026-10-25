package com.cinx.user.profile.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.SaveLearnerProfileCommand;
import com.cinx.user.profile.application.query.LearnerProfileView;
import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.LearnerProfileRepository;
import com.cinx.user.profile.domain.LearnerProfileStatus;
import com.cinx.user.profile.domain.LearningGoal;
import com.cinx.user.profile.domain.User;
import com.cinx.user.profile.domain.UserRepository;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LearnerProfileServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 13, 9, 0);

    @Mock
    private LearnerProfileRepository profiles;
    @Mock
    private UserRepository users;
    @Mock
    private UserEvents userEvents;

    private LearnerProfileService service;

    @BeforeEach
    void setUp() {
        service = new LearnerProfileService(profiles, users, userEvents, Clock.fixed(Instant.parse("2026-10-13T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void aLearnerWithoutAProfileGetsAnEmptyOneWithConsentOn() {
        doReturn(Optional.empty()).when(profiles).findByUserId("u1");

        LearnerProfileView view = service.get("u1");

        assertThat(view.status()).isEqualTo(LearnerProfileStatus.NONE);
        assertThat(view.personalizationConsent()).isTrue();
        assertThat(view.categoryIds()).isEmpty();
    }

    @Test
    void savingCompletesTheProfileAndPublishesItWithTheNewTopics() {
        learnerExists();
        doReturn(Optional.empty()).when(profiles).findByUserId("u1");
        savesWhatItGets();

        LearnerProfileView view = service.save("u1", new SaveLearnerProfileCommand(LearningGoal.CAREER_START, LearnerLevel.BEGINNER,
                45, ContentLanguage.BOTH, List.of(" c1 ", "c2", "c1"), null, null));

        assertThat(view.status()).isEqualTo(LearnerProfileStatus.COMPLETED);
        assertThat(view.categoryIds()).containsExactly("c1", "c2");
        assertThat(view.personalizationConsent()).isTrue();
        assertThat(view.updatedAt()).isEqualTo(NOW);
        ArgumentCaptor<LearnerProfile> published = ArgumentCaptor.forClass(LearnerProfile.class);
        verify(userEvents).publishLearningProfileUpdated(published.capture());
        assertThat(published.getValue().getGoal()).isEqualTo(LearningGoal.CAREER_START);
        verify(userEvents).publishPreferredCategoriesUpdated("u1", List.of("c1", "c2"));
    }

    @Test
    void unchangedTopicsSendNoPreferenceEvent() {
        learnerExists();
        doReturn(Optional.of(profile(LearnerProfileStatus.COMPLETED, List.of("c2", "c1"), true))).when(profiles).findByUserId("u1");
        savesWhatItGets();

        service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, List.of("c1", "c2"), false, null));

        verify(userEvents).publishLearningProfileUpdated(any());
        verify(userEvents, never()).publishPreferredCategoriesUpdated(any(), any());
    }

    @Test
    void consentLeftOutKeepsTheStoredChoice() {
        learnerExists();
        doReturn(Optional.of(profile(LearnerProfileStatus.COMPLETED, List.of(), false))).when(profiles).findByUserId("u1");
        savesWhatItGets();

        assertThat(service.save("u1", new SaveLearnerProfileCommand(LearningGoal.PERSONAL, null, null, null, List.of(), null, null))
                .personalizationConsent()).isFalse();
    }

    @Test
    void aPrivacySwitchAloneDoesNotCompleteAnUnansweredProfile() {
        learnerExists();
        doReturn(Optional.of(profile(LearnerProfileStatus.NONE, List.of(), true))).when(profiles).findByUserId("u1");
        savesWhatItGets();

        assertThat(service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, List.of(), true, false)).status())
                .isEqualTo(LearnerProfileStatus.NONE);
        assertThat(service.save("u1", new SaveLearnerProfileCommand(LearningGoal.SKILL_UP, null, null, null, List.of(), true, false)).status())
                .isEqualTo(LearnerProfileStatus.COMPLETED);
    }

    @Test
    void usageAnalyticsIsOnUntilSwitchedOffAndLeftOutKeepsTheStoredChoice() {
        learnerExists();
        doReturn(Optional.empty()).when(profiles).findByUserId("u1");
        assertThat(service.get("u1").usageAnalyticsConsent()).isTrue();

        doReturn(Optional.of(profile(LearnerProfileStatus.COMPLETED, List.of(), true).toBuilder().usageAnalyticsConsent(false).build()))
                .when(profiles).findByUserId("u1");
        savesWhatItGets();
        assertThat(service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, List.of(), null, null))
                .usageAnalyticsConsent()).isFalse();

        LearnerProfileView switchedOn = service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, List.of(), null, true));
        assertThat(switchedOn.usageAnalyticsConsent()).isTrue();
        ArgumentCaptor<LearnerProfile> published = ArgumentCaptor.forClass(LearnerProfile.class);
        verify(userEvents, times(2)).publishLearningProfileUpdated(published.capture());
        assertThat(published.getValue().isUsageAnalyticsConsent()).isTrue();
    }

    @Test
    void minutesOutsideTheStudyPlanStepsAreRefused() {
        learnerExists();

        assertThatThrownBy(() -> service.save("u1", new SaveLearnerProfileCommand(null, null, 20, null, List.of(), null, null)))
                .isInstanceOf(BadRequestException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void tooManyTopicsAreRefused() {
        learnerExists();
        List<String> many = IntStream.rangeClosed(1, LearnerProfile.MAX_CATEGORIES + 1).mapToObj(i -> "c" + i).toList();

        assertThatThrownBy(() -> service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, many, null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void someoneWithoutAProfileCannotSaveOne() {
        doReturn(Optional.empty()).when(users).findByUserId("u1");

        assertThatThrownBy(() -> service.save("u1", new SaveLearnerProfileCommand(null, null, null, null, List.of(), null, null)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(profiles, userEvents);
    }

    @Test
    void skippingStopsTheInviteButLeavesACompletedProfileAlone() {
        learnerExists();
        doReturn(Optional.empty()).when(profiles).findByUserId("u1");
        savesWhatItGets();
        assertThat(service.skip("u1").status()).isEqualTo(LearnerProfileStatus.SKIPPED);

        doReturn(Optional.of(profile(LearnerProfileStatus.COMPLETED, List.of(), true))).when(profiles).findByUserId("u1");
        assertThat(service.skip("u1").status()).isEqualTo(LearnerProfileStatus.COMPLETED);
        verify(profiles).save(any());
        verifyNoInteractions(userEvents);
    }

    @Test
    void onboardingRecordsAnsweredSkippedAndOlderClientsDifferently() {
        savesWhatItGets();

        service.recordOnboarding("u1", new LearnerProfileAnswers(LearningGoal.CERTIFICATE, null, 30, null), List.of("c1"));
        service.recordOnboarding("u2", new LearnerProfileAnswers(null, null, null, null), List.of());
        service.recordOnboarding("u3", null, List.of("c1"));
        service.recordOnboarding("u4", null, List.of());

        ArgumentCaptor<LearnerProfile> saved = ArgumentCaptor.forClass(LearnerProfile.class);
        verify(profiles, org.mockito.Mockito.times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(LearnerProfile::getStatus)
                .containsExactly(LearnerProfileStatus.COMPLETED, LearnerProfileStatus.SKIPPED, LearnerProfileStatus.NONE);
        assertThat(saved.getAllValues().get(0).getCategoryIds()).containsExactly("c1");
        verify(userEvents, org.mockito.Mockito.times(3)).publishLearningProfileUpdated(any());
    }

    @Test
    void onboardingRefusesMinutesOutsideTheSteps() {
        assertThatThrownBy(() -> service.recordOnboarding("u1", new LearnerProfileAnswers(null, null, 25, null), List.of()))
                .isInstanceOf(BadRequestException.class);
    }

    private void learnerExists() {
        doReturn(Optional.of(User.builder().userId("u1").build())).when(users).findByUserId("u1");
    }

    private void savesWhatItGets() {
        doAnswer(invocation -> invocation.getArgument(0)).when(profiles).save(any());
    }

    private static LearnerProfile profile(LearnerProfileStatus status, List<String> topics, boolean consent) {
        return LearnerProfile.empty("u1").toBuilder().status(status).categoryIds(topics).personalizationConsent(consent)
                .updatedAt(NOW.minusDays(1)).build();
    }
}
