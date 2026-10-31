package com.cinx.learning.progress.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.progress.application.command.TrackingVideoLessonCommand;
import com.cinx.learning.progress.application.query.VideoLessonTrackingHistoryView;
import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoLessonTrackingHistoryMapper extends
        BaseMapper<VideoLessonTrackingHistory, VideoLessonTrackingHistoryView>,
        CreateMapper<VideoLessonTrackingHistory, TrackingVideoLessonCommand>,
        UpdateMapper<VideoLessonTrackingHistory, TrackingVideoLessonCommand> {
}
