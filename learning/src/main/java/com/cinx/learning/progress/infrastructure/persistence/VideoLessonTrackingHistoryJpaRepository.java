package com.cinx.learning.progress.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface VideoLessonTrackingHistoryJpaRepository extends JpaRepository<VideoLessonTrackingHistoryJpaEntity, String> {
    Page<VideoLessonTrackingHistoryJpaEntity> findByVideoLessonId(String videoLessonId, Pageable pageable);

    Optional<VideoLessonTrackingHistoryJpaEntity> findByUserIdAndVideoLessonId(String userId, String videoLessonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM VideoLessonTrackingHistory h WHERE h.userId = :userId AND h.videoLessonId = :videoLessonId")
    Optional<VideoLessonTrackingHistoryJpaEntity> findForUpdateByUserIdAndVideoLessonId(String userId, String videoLessonId);
}
