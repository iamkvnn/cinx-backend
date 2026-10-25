package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import com.cinx.learning.catalog.domain.SubtitleJobType;

public record SubtitleJobView(
        String id,
        SubtitleJobType jobType,
        SubtitleJobStatus status,
        String sourceSubtitleId,
        String outputSubtitleId,
        String sourceLanguageCode,
        String targetLanguageCode,
        String displayName,
        String expectedOutputFileKey,
        Integer progressPercent,
        String errorCode,
        String errorMessage
) {
}

