package com.cinx.learning.catalog.application.query;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record SubtitleWordConfidenceView(
        String subtitleId,
        List<SubtitleWordConfidenceItem> words
) {
    /**
     * @param probability read from the worker's file, which names it "confidence"
     */
    public record SubtitleWordConfidenceItem(
            String word,
            Double start,
            Double end,
            @JsonAlias("confidence") Double probability
    ) {
    }
}
