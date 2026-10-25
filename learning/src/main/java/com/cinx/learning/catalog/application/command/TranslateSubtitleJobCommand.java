package com.cinx.learning.catalog.application.command;

import java.util.List;

public record TranslateSubtitleJobCommand(
        String sourceSubtitleId,
        List< String> targetLanguageCodes
) {
}

