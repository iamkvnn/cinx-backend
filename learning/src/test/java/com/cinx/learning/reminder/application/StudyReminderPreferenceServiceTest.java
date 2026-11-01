package com.cinx.learning.reminder.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
import com.cinx.learning.reminder.domain.StudyReminderPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyReminderPreferenceServiceTest {
    @Mock private StudyReminderPreferenceRepository preferences;

    private StudyReminderPreferenceService service() {
        return new StudyReminderPreferenceService(Clock.fixed(Instant.parse("2026-10-13T00:00:00Z"), ZoneOffset.UTC), preferences);
    }

    @Test
    void aLearnerWithoutAPreferenceIsRemindedAt19() {
        when(preferences.findByUserId("u1")).thenReturn(Optional.empty());

        StudyReminderPreference preference = service().get("u1");

        assertThat(preference.isEnabled()).isTrue();
        assertThat(preference.getHour()).isEqualTo(19);
    }

    @Test
    void anHourInTheQuietHoursIsRefused() {
        assertThatThrownBy(() -> service().update("u1", true, 21)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service().update("u1", true, 7)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(preferences);
    }

    @Test
    void theLearnersOwnChoiceIsStored() {
        when(preferences.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StudyReminderPreference saved = service().update("u1", false, 8);

        assertThat(saved.getUserId()).isEqualTo("u1");
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getHour()).isEqualTo(8);
    }
}
