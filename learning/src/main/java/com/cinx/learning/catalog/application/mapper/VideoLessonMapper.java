package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateVideoLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoLessonCommand;
import com.cinx.learning.catalog.application.query.VideoLessonView;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.catalog.domain.VideoLesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = SubtitleTrackMapper.class)
public interface VideoLessonMapper extends
        BaseMapper<VideoLesson, VideoLessonView>,
        CreateMapper<VideoLesson, CreateVideoLessonCommand>,
        UpdateMapper<VideoLesson, UpdateVideoLessonCommand> {

    @Override
    @Mapping(target = "hasQuestions", ignore = true)
    @Mapping(target = "questionCount", ignore = true)
    @Mapping(target = "hasSubtitles", ignore = true)
    @Mapping(target = "subtitleCount", ignore = true)
    @Mapping(target = "subtitles", ignore = true)
    VideoLessonView toDto(VideoLesson model);

    /** Questions and subtitles live in their own repositories, so the caller hands in what it loaded. */
    @Mapping(target = "hasQuestions", expression = "java(questionCount > 0)")
    @Mapping(target = "questionCount", source = "questionCount")
    @Mapping(target = "hasSubtitles", expression = "java(subtitles != null && !subtitles.isEmpty())")
    @Mapping(target = "subtitleCount", expression = "java(subtitles != null ? subtitles.size() : 0)")
    @Mapping(target = "subtitles", source = "subtitles")
    VideoLessonView toDto(VideoLesson model, List<SubtitleTrack> subtitles, int questionCount);
}

