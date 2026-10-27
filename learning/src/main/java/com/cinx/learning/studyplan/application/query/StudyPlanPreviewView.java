package com.cinx.learning.studyplan.application.query;

import java.time.LocalDate;

/** The finish date the given settings would give, without saving them. */
public record StudyPlanPreviewView(LocalDate finishDate, int remainingMinutes, int remainingLessons) {
}
