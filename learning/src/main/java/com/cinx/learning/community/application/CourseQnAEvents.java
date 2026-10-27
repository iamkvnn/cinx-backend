package com.cinx.learning.community.application;

import com.cinx.learning.community.application.event.CourseAnswerCreatedEvent;
import com.cinx.learning.community.application.event.CourseQuestionCreatedEvent;

/** Port: records the question and answer events in the caller's transaction, for the outbox relay. */
public interface CourseQnAEvents {
    void publishQuestionCreatedEvent(CourseQuestionCreatedEvent event);

    void publishAnswerCreatedEvent(CourseAnswerCreatedEvent event);
}
