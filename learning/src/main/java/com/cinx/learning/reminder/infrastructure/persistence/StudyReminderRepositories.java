package com.cinx.learning.reminder.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

interface StudyReminderPreferenceJpaRepository extends JpaRepository<StudyReminderPreferenceJpaEntity, String> {
}

interface StudyReminderLogJpaRepository extends JpaRepository<StudyReminderLogJpaEntity, String> {
    boolean existsByUserIdAndReminderDate(String userId, LocalDate reminderDate);

    long countByUserIdAndReminderDateBetween(String userId, LocalDate from, LocalDate to);
}
