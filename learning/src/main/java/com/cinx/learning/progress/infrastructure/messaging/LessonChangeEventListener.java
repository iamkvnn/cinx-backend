package com.cinx.learning.progress.infrastructure.messaging;

import com.cinx.learning.progress.infrastructure.messaging.event.CourseContentPublishedEvent;
import com.cinx.learning.progress.application.ILearningProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeEventListener {

    private final ILearningProgressService learningProgressService;

    @RabbitListener(queues = "learning.lesson-change.queue",
                    containerFactory = "rabbitListenerContainerFactory")
    public void onCourseContentPublished(CourseContentPublishedEvent event) {
        log.info("Received CourseContentPublishedEvent: courseId={}", event.getCourseId());
        learningProgressService.recomputeCourseProgress(event.getCourseId());
    }
}
