package com.cinx.learning.catalog.application.query;

import java.util.List;

public record VideoLessonView (
        String videoUrl,
        String fileName,
        String fileType,
        Long fileSize,
        Integer duration,
        String status,
        Boolean hasQuestions,
        Integer questionCount,
        Boolean hasSubtitles,
        Integer subtitleCount,
        List<SubtitleTrackView> subtitles
){
}

