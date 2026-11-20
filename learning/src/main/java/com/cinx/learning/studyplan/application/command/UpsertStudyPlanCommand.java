package com.cinx.learning.studyplan.application.command;

import com.cinx.learning.studyplan.domain.StudyPlanSource;

import java.util.List;

public record UpsertStudyPlanCommand(
        StudyPlanSource source,

        List< String> courseIds,

        List<   Integer> freeDays,

        Integer minutesPerDay
) {
}
