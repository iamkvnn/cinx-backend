package com.cinx.learning.reminder.infrastructure.persistence;

import com.cinx.learning.reminder.domain.StudyReminderLog;
import com.cinx.learning.reminder.domain.StudyReminderLogRepository;
import com.cinx.learning.reminder.domain.StudyReminderPreference;
import com.cinx.learning.reminder.domain.StudyReminderPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudyReminderPreferenceRepositoryAdapter implements StudyReminderPreferenceRepository {
    private final StudyReminderPreferenceJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<StudyReminderPreference> findByUserId(String userId) {
        return jpa.findById(userId).map(StudyReminderPreferenceRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyReminderPreference> findByUserIds(Collection<String> userIds) {
        return jpa.findAllById(userIds).stream().map(StudyReminderPreferenceRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public StudyReminderPreference save(StudyReminderPreference preference) {
        StudyReminderPreferenceJpaEntity row = jpa.findById(preference.getUserId()).orElseGet(StudyReminderPreferenceJpaEntity::new);
        row.setUserId(preference.getUserId());
        row.setEnabled(preference.isEnabled());
        row.setHour(preference.getHour());
        row.setUpdatedAt(preference.getUpdatedAt());
        return toDomain(jpa.save(row));
    }

    private static StudyReminderPreference toDomain(StudyReminderPreferenceJpaEntity row) {
        return StudyReminderPreference.builder()
                .userId(row.getUserId()).enabled(row.isEnabled()).hour(row.getHour()).updatedAt(row.getUpdatedAt()).build();
    }
}

@Component
@RequiredArgsConstructor
class StudyReminderLogRepositoryAdapter implements StudyReminderLogRepository {
    private final StudyReminderLogJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndReminderDate(String userId, LocalDate reminderDate) {
        return jpa.existsByUserIdAndReminderDate(userId, reminderDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndReminderDateBetween(String userId, LocalDate from, LocalDate to) {
        return jpa.countByUserIdAndReminderDateBetween(userId, from, to);
    }

    @Override
    @Transactional
    public void insert(StudyReminderLog log) {
        StudyReminderLogJpaEntity row = new StudyReminderLogJpaEntity();
        row.setId(log.getId());
        row.setUserId(log.getUserId());
        row.setReminderDate(log.getReminderDate());
        row.setReason(log.getReason());
        row.setWrittenBy(log.getWrittenBy());
        row.setCreatedAt(log.getCreatedAt());
        // flushed now: a duplicate (learner, day) fails here instead of at commit
        jpa.saveAndFlush(row);
    }
}
