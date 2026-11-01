package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.mapper.UserStreakMapper;
import com.cinx.learning.engagement.domain.UserStreak;
import com.cinx.learning.engagement.domain.UserStreakRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {
    @Mock
    private UserStreakRepository streaks;
    @Mock
    private UserStreakMapper mapper;

    @Test
    void activityAfterMidnightInVietnamCountsForTheNewVietnamDay() {
        // 17:30 UTC on the 13th is 00:30 on the 14th in Vietnam
        StreakService service = at("2026-10-13T17:30:00Z");
        when(streaks.findForUpdateByUserId("u1")).thenReturn(Optional.of(streak(3, LocalDate.of(2026, 10, 13))));

        service.updateStreakOnActivity("u1");

        UserStreak saved = saved();
        assertThat(saved.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(saved.getCurrentStreak()).isEqualTo(4);
    }

    @Test
    void twoActivitiesOnTheSameVietnamDayCountOnce() {
        // 23:00 UTC on the 12th and 10:00 UTC on the 13th are both the 13th in Vietnam
        when(streaks.findForUpdateByUserId("u1")).thenReturn(Optional.of(streak(5, LocalDate.of(2026, 10, 13))));

        at("2026-10-12T23:00:00Z").updateStreakOnActivity("u1");

        UserStreak saved = saved();
        assertThat(saved.getCurrentStreak()).isEqualTo(5);
        assertThat(saved.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 10, 13));
    }

    private StreakService at(String instant) {
        return new StreakService(streaks, mapper, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private UserStreak saved() {
        ArgumentCaptor<UserStreak> captor = ArgumentCaptor.forClass(UserStreak.class);
        verify(streaks).save(captor.capture());
        return captor.getValue();
    }

    private static UserStreak streak(int current, LocalDate last) {
        return UserStreak.builder().userId("u1").currentStreak(current).highestStreak(current).lastActivityDate(last).build();
    }
}
