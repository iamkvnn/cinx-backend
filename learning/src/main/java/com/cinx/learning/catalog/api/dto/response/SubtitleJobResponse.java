package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import com.cinx.learning.catalog.domain.SubtitleJobType;
import io.swagger.v3.oas.annotations.media.Schema;

public record SubtitleJobResponse(
        @Schema(example = "job_123")
        String id,
        @Schema(example = "GENERATE_DEFAULT")
        SubtitleJobType jobType,
        @Schema(example = "QUEUED")
        SubtitleJobStatus status,
        @Schema(example = "sub_source_123")
        String sourceSubtitleId,
        @Schema(example = "sub_output_123")
        String outputSubtitleId,
        @Schema(example = "vi")
        String sourceLanguageCode,
        @Schema(example = "en")
        String targetLanguageCode,
        @Schema(example = "English")
        String displayName,
        @Schema(example = "courses/subtitles/ai/lesson-1/en/job-123.vtt")
        String expectedOutputFileKey,
        @Schema(example = "50")
        Integer progressPercent,
        @Schema(example = "AI_SUBTITLE_FAILED")
        String errorCode,
        @Schema(example = "Transcription failed")
        String errorMessage
) {
}

