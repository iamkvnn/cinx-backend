package com.cinx.user.profile.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Test
    void aNewProfileIsNotAnInstructorAndHasNoApplication() {
        User user = User.builder().userId("u").build();

        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.NONE);
        assertThat(user.getIsPartnershipTerminated()).isFalse();
        assertThat(user.getIsReceivePushNotification()).isFalse();
        assertThat(user.isActiveInstructor()).isFalse();
        assertThat(user.hasActiveInstructorApplication()).isFalse();
    }

    @Test
    void profileChangesSkipEveryNullField() {
        User user = User.builder().userId("u").name("Old").gender(Gender.MALE).phoneNumber("1").bio("b").avatarFileKey("a").cvFileKey("c").build();

        user.applyProfileChanges("New", null, null, null, true, null, null);

        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getIsReceivePushNotification()).isTrue();
        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getPhoneNumber()).isEqualTo("1");
        assertThat(user.getBio()).isEqualTo("b");
        assertThat(user.getAvatarFileKey()).isEqualTo("a");
        assertThat(user.getCvFileKey()).isEqualTo("c");
    }

    @Test
    void anInstructorIsActiveOnlyWhileVerifiedAndNotTerminated() {
        assertThat(User.builder().isInstructorVerified(true).build().isActiveInstructor()).isTrue();
        assertThat(User.builder().isInstructorVerified(true).isPartnershipTerminated(true).build().isActiveInstructor()).isFalse();
        assertThat(User.builder().isInstructorVerified(false).build().isActiveInstructor()).isFalse();
        assertThat(User.builder().isInstructorVerified(null).build().isActiveInstructor()).isFalse();
    }

    @Test
    void anApplicationBlocksANewOneWhilePendingOrAfterVerification() {
        assertThat(User.builder().instructorApplicationStatus(InstructorApplicationStatus.PENDING).build().hasActiveInstructorApplication()).isTrue();
        assertThat(User.builder().isInstructorVerified(true).build().hasActiveInstructorApplication()).isTrue();
        assertThat(User.builder().instructorApplicationStatus(InstructorApplicationStatus.REJECTED).build().hasActiveInstructorApplication()).isFalse();
    }

    @Test
    void approvingClearsAnEarlierRejectionReason() {
        User user = User.builder().instructorApplicationRejectionReason("old reason").build();

        user.approveAsInstructor(NOW);

        assertThat(user.getInstructorApplicationRejectionReason()).isNull();
        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.APPROVED);
        assertThat(user.getInstructorVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectingATrimsTheReasonAndTakesAwayVerification() {
        User user = User.builder().isInstructorVerified(true).build();

        user.rejectInstructorApplication("  not enough experience ", NOW);

        assertThat(user.getInstructorApplicationRejectionReason()).isEqualTo("not enough experience");
        assertThat(user.getIsInstructorVerified()).isFalse();
        assertThat(user.getInstructorApplicationReviewedAt()).isEqualTo(NOW);
    }

    @Test
    void terminatingTreatsABlankDetailAsNone() {
        User user = User.builder().isInstructorVerified(true).build();

        user.terminatePartnership(PartnershipTerminationReasonType.POLICY_VIOLATION, "   ", NOW);

        assertThat(user.getPartnershipTerminationReasonDetail()).isNull();
        assertThat(user.getIsPartnershipTerminated()).isTrue();
        assertThat(user.getIsInstructorVerified()).isFalse();
    }

    @Test
    void resubmittingClearsTheVerdictAndReopensThePartnership() {
        User user = User.builder().instructorApplicationStatus(InstructorApplicationStatus.REJECTED)
                .instructorApplicationReviewedAt(NOW.minusDays(2)).instructorApplicationRejectionReason("r").isPartnershipTerminated(true).build();

        user.resubmitInstructorApplication(NOW);

        assertThat(user.getInstructorApplicationStatus()).isEqualTo(InstructorApplicationStatus.PENDING);
        assertThat(user.getInstructorApplicationSubmittedAt()).isEqualTo(NOW);
        assertThat(user.getInstructorApplicationReviewedAt()).isNull();
        assertThat(user.getInstructorApplicationRejectionReason()).isNull();
        assertThat(user.getIsPartnershipTerminated()).isFalse();
    }
}
