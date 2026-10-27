package com.cinx.learning.studyplan.application;

import com.cinx.learning.studyplan.application.command.UpsertStudyPlanCommand;
import com.cinx.learning.studyplan.application.query.DayOffView;
import com.cinx.learning.studyplan.application.query.LastActivityView;
import com.cinx.learning.studyplan.application.query.StudyPlanPreviewView;
import com.cinx.learning.studyplan.application.query.StudyPlanView;

import java.time.LocalDate;
import java.util.List;

public interface IStudyPlanService {
    /** 404 when the learner has no plan. {@code weekStart} null = this week; any day is snapped to its Monday. */
    StudyPlanView get(String userId, LocalDate weekStart);

    StudyPlanView upsert(String userId, UpsertStudyPlanCommand request);

    StudyPlanPreviewView preview(String userId, UpsertStudyPlanCommand request);

    DayOffView addDayOff(String userId, LocalDate date);

    StudyPlanView removeDayOff(String userId, LocalDate date);

    /** Points a plan that follows the learner's path at {@code pathId}. No plan, or a plan over courses: nothing. */
    void attachPath(String userId, String pathId);

    /** Every learner who has a plan, for the study reminders. */
    List<String> userIdsWithPlan();

    /** 404 when no heartbeat has named a lesson yet. */
    /** The lesson to resume; with a courseId, only activity in that course counts. */
    LastActivityView lastActivity(String userId, String courseId);
}
