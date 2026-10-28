package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record VideoLessonResponse (
        @Schema(example = "https://example.com/video.mp4")
        String videoUrl,
        @Schema(example = "lecture.mp4")
        String fileName,
        @Schema(example = "video/mp4")
        String fileType,
        @Schema(example = "104857600")
        Long fileSize,
        @Schema(example = "3600")
        Integer duration,
        @Schema(example = "READY")
        String status,
        @Schema(example = "true")
        Boolean hasQuestions,
        @Schema(example = "5")
        Integer questionCount,
        @Schema(example = "true")
        Boolean hasSubtitles,
        @Schema(example = "2")
        Integer subtitleCount,
        List<SubtitleTrackResponse> subtitles
) {
}
