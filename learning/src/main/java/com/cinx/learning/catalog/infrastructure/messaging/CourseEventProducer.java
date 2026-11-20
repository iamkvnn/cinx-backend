package com.cinx.learning.catalog.infrastructure.messaging;

import com.cinx.learning.catalog.application.CourseEvents;

import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;
import com.cinx.learning.catalog.application.event.CourseRecommendationEvent;
import com.cinx.learning.catalog.application.event.CourseApprovalRequestedEvent;
import com.cinx.learning.catalog.application.event.CourseCollaboratorInvitedEvent;
import com.cinx.learning.catalog.application.event.CourseContentPublishedEvent;
import com.cinx.learning.catalog.application.event.LessonChangedEvent;
import com.cinx.learning.catalog.application.event.QuizSyncEvent;
import com.cinx.learning.catalog.application.event.ScoringModeChangedEvent;
import com.cinx.learning.catalog.application.event.SubtitleGenerateRequestedEvent;
import com.cinx.learning.catalog.application.event.SubtitleTranslateRequestedEvent;
import com.cinx.learning.catalog.application.event.CourseSellabilityChanged;
import com.cinx.learning.catalog.application.event.CourseTranscriptsChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseEventProducer implements CourseEvents {
    private static final String EXCHANGE = "learning.events.exchange";
    private static final String DOMAIN_EXCHANGE = "cinx.domain.events";

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishLessonChangedEvent(LessonChangedEvent event) {
        System.out.println("Publishing lesson updated event: " + event);
        outboxEventPublisher.enqueue(
                event.getCourseId() + "-" + event.getLessonId() + "-" + event.getChangeType(),
                "Lesson",
                event.getLessonId(),
                "LessonChanged",
                EXCHANGE,
                "course.lesson.changed",
                event
        );
    }

    public void publishCourseContentPublishedEvent(CourseContentPublishedEvent event) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "Course",
                event.getCourseId(),
                "CourseContentPublished",
                EXCHANGE,
                "course.content.published",
                event
        );
    }

    public void publishCourseApprovalRequestedEvent(CourseApprovalRequestedEvent event) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "Course",
                event.getCourseId(),
                "CourseApprovalRequested",
                EXCHANGE,
                "course.approval.requested",
                event
        );
    }

    public void publishCollaboratorInvitedEvent(CourseCollaboratorInvitedEvent event) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "Course",
                event.getCourseId(),
                "CourseCollaboratorInvited",
                EXCHANGE,
                "course.collaborator.invited",
                event
        );
    }

    public void publishQuizSyncEvent(QuizSyncEvent event) {
        System.out.println("Publishing quiz sync event: quizLessonId=" + event.getQuizLessonId());
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "QuizLesson",
                event.getQuizLessonId(),
                "QuizSyncAndRegrade",
                EXCHANGE,
                "course.quiz.sync-and-regrade",
                event
        );
    }

    public void publishScoringModeChangedEvent(ScoringModeChangedEvent event) {
        System.out.println("Publishing quiz sync event: quizLessonId=" + event.quizLessonId());
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "QuizLesson",
                event.quizLessonId(),
                "QuizScoringModeChanged",
                EXCHANGE,
                "course.quiz.scoring-mode-changed",
                event
        );
    }

    public void publishCourseRecommendationEvent(String routingKey, String eventType, CourseRecommendationEvent event) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "Course",
                event.course().id(),
                eventType,
                EXCHANGE,
                routingKey,
                event
        );
    }

    public void publishSubtitleGenerateRequestedEvent(SubtitleGenerateRequestedEvent event) {
        outboxEventPublisher.enqueue(
                event.jobId(),
                "SubtitleJob",
                event.jobId(),
                "SubtitleGenerateRequested",
                EXCHANGE,
                "learning.subtitle.generate.requested",
                event
        );
    }

    public void publishSubtitleTranslateRequestedEvent(SubtitleTranslateRequestedEvent event) {
        outboxEventPublisher.enqueue(
                event.jobId(),
                "SubtitleJob",
                event.jobId(),
                "SubtitleTranslateRequested",
                EXCHANGE,
                "learning.subtitle.translate.requested",
                event
        );
    }

    public void publishSellabilityChanged(CourseSellabilityChanged event) {
        outboxEventPublisher.enqueue(event.eventId(), "Course", event.courseId(),
                "CourseSellabilityChanged", DOMAIN_EXCHANGE,
                "learning.course.sellability.changed", event);
    }

    /** A random eventId: the outbox drops a repeated id, and the same course changes again and again. */
    public void publishTranscriptsChanged(CourseTranscriptsChanged event) {
        outboxEventPublisher.enqueue(event.eventId(), "Course", event.courseId(),
                "CourseTranscriptsChanged", DOMAIN_EXCHANGE,
                "learning.course.transcripts.changed", event);
    }
}

