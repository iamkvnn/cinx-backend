package com.cinx.learning.reminder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** When a learner wants a study reminder (Vietnam time). A learner without a stored one gets {@link #defaultFor}. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyReminderPreference {
    public static final int FIRST_HOUR = 8;
    /** The last hour a reminder may go out; quiet hours start at 21:00. */
    public static final int LAST_HOUR = 20;
    public static final int DEFAULT_HOUR = 19;

    private String userId;
    private boolean enabled;
    private int hour;
    private Instant updatedAt;

    public static StudyReminderPreference defaultFor(String userId) {
        return StudyReminderPreference.builder().userId(userId).enabled(true).hour(DEFAULT_HOUR).build();
    }
}
