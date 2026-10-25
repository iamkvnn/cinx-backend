package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearningGoal;

/** The onboarding step's answers; an empty object means the learner skipped the step. */
public record LearnerProfileAnswers(
        LearningGoal goal,
        LearnerLevel level,
        Integer minutesPerDay,
        ContentLanguage contentLanguage) {

    public boolean answered() {
        return goal != null || level != null || minutesPerDay != null || contentLanguage != null;
    }
}
