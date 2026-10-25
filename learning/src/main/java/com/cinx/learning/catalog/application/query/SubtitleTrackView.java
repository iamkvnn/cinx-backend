package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.SubtitleFormat;
import com.cinx.learning.catalog.domain.SubtitleSource;
import com.cinx.learning.catalog.domain.SubtitleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record SubtitleTrackView(
        String id,
        String languageCode,
        String displayName,
        String fileUrl,
        String fileKey,
        String wordConfidenceFileKey,
        String wordConfidenceFileUrl,
        String fileName,
        String fileType,
        Long fileSize,
        SubtitleFormat format,
        SubtitleSource source,
        SubtitleStatus status,
        Boolean isDefault,
        Map<String, Object> qualityReport,
        String reviewedBy,
        LocalDateTime reviewedAt,
        BigDecimal editedCueShare
) {
}

