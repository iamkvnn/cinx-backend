package com.cinx.learning.reminder.api.dto.response;

import com.cinx.learning.reminder.domain.StudyReminderPreference;

public record StudyReminderResponse(boolean enabled, int hour) {
    public static StudyReminderResponse of(StudyReminderPreference preference) {
        return new StudyReminderResponse(preference.isEnabled(), preference.getHour());
    }
}
