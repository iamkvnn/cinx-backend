package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record SubtitleTrackResponse(
        @Schema(example = "sub_123")
        String id,
        @Schema(example = "vi")
        String languageCode,
        @Schema(example = "Vietnamese")
        String displayName,
        @Schema(example = "https://cdn.example.com/courses/subtitles/lesson-1/vi/sub_123.vtt")
        String fileUrl,
        @Schema(example = "courses/subtitles/lesson-1/vi/sub_123.vtt")
        String fileKey,
        @Schema(example = "courses/subtitles/ai/lesson-1/vi/job-123.words.json")
        String wordConfidenceFileKey,
        @Schema(example = "https://cdn.example.com/courses/subtitles/ai/lesson-1/vi/job-123.words.json")
        String wordConfidenceFileUrl,
        @Schema(example = "intro.vi.srt")
        String fileName,
        @Schema(example = "text/vtt")
        String fileType,
        @Schema(example = "4096")
        Long fileSize,
        @Schema(example = "VTT")
        SubtitleFormat format,
        @Schema(example = "MANUAL")
        SubtitleSource source,
        @Schema(example = "READY", description = "REVIEW_REQUIRED: AI output that learners do not see until an instructor approves it")
        SubtitleStatus status,
        @Schema(example = "true")
        Boolean isDefault,
        @Schema(description = "The subtitle worker's quality summary (schemaVersion, thresholds, metrics, warnings); null for a track a person made")
        Map<String, Object> qualityReport,
        @Schema(example = "user_123", description = "Who approved the AI output; null when not reviewed")
        String reviewedBy,
        @Schema(example = "2026-10-13T09:00:00")
        LocalDateTime reviewedAt,
        @Schema(example = "0.125", description = "Share of the AI cues the reviewer changed before approving")
        BigDecimal editedCueShare
) {
}

