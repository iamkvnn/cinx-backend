package com.cinx.learning.reminder.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "StudyReminderPreference")
@Table(name = "study_reminder_preference")
@Getter
@Setter
@NoArgsConstructor
class StudyReminderPreferenceJpaEntity {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "reminder_hour", nullable = false)
    private int hour;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
