package com.cinx.learning.reminder.infrastructure.persistence;

import com.cinx.learning.reminder.domain.ReminderAuthor;
import com.cinx.learning.reminder.domain.ReminderReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity(name = "StudyReminderLog")
@Table(name = "study_reminder_log")
@Getter
@Setter
@NoArgsConstructor
class StudyReminderLogJpaEntity {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReminderReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "written_by", nullable = false, length = 20)
    private ReminderAuthor writtenBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
