package com.cinx.learning.reminder.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where learners' reminder preferences are stored. */
public interface StudyReminderPreferenceRepository {
    Optional<StudyReminderPreference> findByUserId(String userId);

    List<StudyReminderPreference> findByUserIds(Collection<String> userIds);

    StudyReminderPreference save(StudyReminderPreference preference);
}
