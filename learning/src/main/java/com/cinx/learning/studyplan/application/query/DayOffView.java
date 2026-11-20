package com.cinx.learning.studyplan.application.query;

/** After marking a day busy: how many planned sessions moved, and the plan's current week. */
public record DayOffView(
         int moved,
        StudyPlanView plan
) {
}
