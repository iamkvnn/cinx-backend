package com.cinx.learning.progress.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.util.Optional;

/** Port: where VideoLessonTrackingHistory rows are stored. */
public interface VideoLessonTrackingHistoryRepository {
    PageResult<VideoLessonTrackingHistory> findByVideoLessonId(String videoLessonId, PageQuery pageable);

    Optional<VideoLessonTrackingHistory> findByUserIdAndVideoLessonId(String userId, String videoLessonId);

    Optional<VideoLessonTrackingHistory> findForUpdateByUserIdAndVideoLessonId(String userId, String videoLessonId);

    VideoLessonTrackingHistory save(VideoLessonTrackingHistory entity);
}
