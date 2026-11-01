package com.cinx.learning.reminder.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
import com.cinx.learning.reminder.domain.StudyReminderPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** A learner's own reminder switch and hour; nobody else's can be read or changed. */
@Service
@RequiredArgsConstructor
public class StudyReminderPreferenceService {
    private final Clock clock;
    private final StudyReminderPreferenceRepository preferences;

    @Transactional(readOnly = true)
    public StudyReminderPreference get(String userId) {
        return preferences.findByUserId(userId).orElseGet(() -> StudyReminderPreference.defaultFor(userId));
    }

    @Transactional
    public StudyReminderPreference update(String userId, boolean enabled, int hour) {
        if (hour < StudyReminderPreference.FIRST_HOUR || hour > StudyReminderPreference.LAST_HOUR) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A reminder hour is between "
                    + StudyReminderPreference.FIRST_HOUR + " and " + StudyReminderPreference.LAST_HOUR);
        }
        return preferences.save(StudyReminderPreference.builder()
                .userId(userId).enabled(enabled).hour(hour).updatedAt(clock.instant()).build());
    }
}
