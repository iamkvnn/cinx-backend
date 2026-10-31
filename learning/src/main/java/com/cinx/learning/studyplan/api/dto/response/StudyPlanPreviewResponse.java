package com.cinx.learning.studyplan.api.dto.response;

import java.time.LocalDate;

/** The finish date the given settings would give, without saving them. */
public record StudyPlanPreviewResponse(LocalDate finishDate, int remainingMinutes, int remainingLessons) {
}
