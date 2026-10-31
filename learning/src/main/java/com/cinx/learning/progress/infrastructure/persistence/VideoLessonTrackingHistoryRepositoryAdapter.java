package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistoryRepository;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class VideoLessonTrackingHistoryRepositoryAdapter implements VideoLessonTrackingHistoryRepository {
    private final VideoLessonTrackingHistoryJpaRepository jpa;
    private final VideoLessonTrackingHistoryPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<VideoLessonTrackingHistory> findByVideoLessonId(String videoLessonId, PageQuery pageable) {
        return Pages.result(jpa.findByVideoLessonId(videoLessonId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoLessonTrackingHistory> findByUserIdAndVideoLessonId(String userId, String videoLessonId) {
        return jpa.findByUserIdAndVideoLessonId(userId, videoLessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<VideoLessonTrackingHistory> findForUpdateByUserIdAndVideoLessonId(String userId, String videoLessonId) {
        return jpa.findForUpdateByUserIdAndVideoLessonId(userId, videoLessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public VideoLessonTrackingHistory save(VideoLessonTrackingHistory entity) {
        return store(entity);
    }

    /** Inserts a new VideoLessonTrackingHistory, or writes the changes onto the stored row and leaves the lock version to the store. */
    private VideoLessonTrackingHistory store(VideoLessonTrackingHistory entity) {
        VideoLessonTrackingHistoryJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
