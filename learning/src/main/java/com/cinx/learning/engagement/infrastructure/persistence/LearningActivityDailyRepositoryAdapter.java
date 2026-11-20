package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.LearningActivityDaily;
import com.cinx.learning.engagement.domain.LearningActivityDailyRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LearningActivityDailyRepositoryAdapter implements LearningActivityDailyRepository {
    private final LearningActivityDailyJpaRepository jpa;
    private final LearningActivityDailyPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<LearnerActivity> activityPerLearner(String courseId) {
        return jpa.activityPerLearner(courseId).stream().map(LearningActivityDailyRepositoryAdapter::wrapLearnerActivity).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LearningActivityDaily> findFirstByUserIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId) {
        return jpa.findFirstByUserIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LearningActivityDaily> findFirstByUserIdAndCourseIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId, String courseId) {
        return jpa.findFirstByUserIdAndCourseIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(userId, courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningActivityDaily> findLatestWithItemPerCourse(String userId, Collection<String> courseIds) {
        return jpa.findLatestWithItemPerCourse(userId, courseIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public Optional<LearningActivityDaily> findByUserIdAndCourseIdAndActivityDate(String userId, String courseId, LocalDate activityDate) {
        return jpa.findByUserIdAndCourseIdAndActivityDate(userId, courseId, activityDate).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Long sumActiveSecondsByUserId(String userId) {
        return jpa.sumActiveSecondsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserIdsLastActiveBetween(LocalDate from, LocalDate to) {
        return jpa.findUserIdsLastActiveBetween(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> findLastActivityDate(String userId) {
        return Optional.ofNullable(jpa.findLastActivityDate(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndActivityDate(String userId, LocalDate activityDate) {
        return jpa.existsByUserIdAndActivityDate(userId, activityDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateUserActivityByMonth(String userId, LocalDate startDate, LocalDate endDate) {
        return jpa.aggregateUserActivityByMonth(userId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateUserActivityByDay(String userId, LocalDate startDate, LocalDate endDate) {
        return jpa.aggregateUserActivityByDay(userId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long sumActiveSecondsByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate) {
        return jpa.sumActiveSecondsByCourseIdBetween(courseId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveLearnersByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate) {
        return jpa.countActiveLearnersByCourseIdBetween(courseId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCourseActivityByMonth(String courseId, LocalDate startDate, LocalDate endDate) {
        return jpa.aggregateCourseActivityByMonth(courseId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateCourseActivityByDay(String courseId, LocalDate startDate, LocalDate endDate) {
        return jpa.aggregateCourseActivityByDay(courseId, startDate, endDate);
    }

    @Override
    @Transactional
    public LearningActivityDaily save(LearningActivityDaily entity) {
        return store(entity);
    }

    /** Inserts a new LearningActivityDaily, or writes the changes onto the stored row and leaves the lock version to the store. */
    private LearningActivityDaily store(LearningActivityDaily entity) {
        LearningActivityDailyJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static LearningActivityDailyRepository.LearnerActivity wrapLearnerActivity(LearningActivityDailyJpaRepository.LearnerActivity row) {
        if (row == null) {
            return null;
        }
        return new LearningActivityDailyRepository.LearnerActivity() {
            @Override
            public String getUserId() {
                return row.getUserId();
            }
            @Override
            public java.time.LocalDateTime getLastActivityAt() {
                return row.getLastActivityAt();
            }
            @Override
            public Long getActiveSeconds() {
                return row.getActiveSeconds();
            }
        };
    }
}
