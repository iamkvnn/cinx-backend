package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.event.CourseApprovalRequestedEvent;
import com.cinx.learning.catalog.application.event.CourseCollaboratorInvitedEvent;
import com.cinx.learning.catalog.application.event.CourseContentPublishedEvent;
import com.cinx.learning.catalog.application.event.CourseRecommendationEvent;
import com.cinx.learning.catalog.application.event.CourseSellabilityChanged;
import com.cinx.learning.catalog.application.event.CourseTranscriptsChanged;
import com.cinx.learning.catalog.application.event.LessonChangedEvent;
import com.cinx.learning.catalog.application.event.QuizSyncEvent;
import com.cinx.learning.catalog.application.event.ScoringModeChangedEvent;
import com.cinx.learning.catalog.application.event.SubtitleGenerateRequestedEvent;
import com.cinx.learning.catalog.application.event.SubtitleTranslateRequestedEvent;

/** Port: records the events the catalog publishes, in the caller's transaction, for the outbox relay. */
public interface CourseEvents {
    void publishLessonChangedEvent(LessonChangedEvent event);

    void publishCourseContentPublishedEvent(CourseContentPublishedEvent event);

    void publishCourseApprovalRequestedEvent(CourseApprovalRequestedEvent event);

    void publishCollaboratorInvitedEvent(CourseCollaboratorInvitedEvent event);

    void publishQuizSyncEvent(QuizSyncEvent event);

    void publishScoringModeChangedEvent(ScoringModeChangedEvent event);

    void publishCourseRecommendationEvent(String routingKey, String eventType, CourseRecommendationEvent event);

    void publishSubtitleGenerateRequestedEvent(SubtitleGenerateRequestedEvent event);

    void publishSubtitleTranslateRequestedEvent(SubtitleTranslateRequestedEvent event);

    void publishSellabilityChanged(CourseSellabilityChanged event);

    void publishTranscriptsChanged(CourseTranscriptsChanged event);
}
