package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.VideoQuizQuestionType;

import java.util.List;

public record VideoQuestionView(
        String id,
        String questionText,
        VideoQuizQuestionType questionType,
        Integer timestampSeconds,
        List<VideoOptionView> options
) {
}
