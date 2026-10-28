package com.cinx.learning.catalog.infrastructure.messaging;

import com.cinx.learning.catalog.domain.CourseEnrollmentEvent;
import com.cinx.learning.catalog.domain.CourseEnrollmentEventRepository;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.infrastructure.messaging.event.LearningEnrollmentGranted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {
    private final CourseEnrollmentEventRepository eventRepository;
    private final ICourseService courseService;

    @Transactional
    @RabbitListener(queues = "learning.catalog-enrollment-granted.queue")
    public void onEnrollmentGranted(LearningEnrollmentGranted event) {
        String id = event.userId() + ":" + event.courseId();
        try {
            eventRepository.saveAndFlush(CourseEnrollmentEvent.builder()
                    .id(id)
                    .userId(event.userId())
                    .courseId(event.courseId())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Enrollment event already applied: {}", id);
            return;
        }
        courseService.increaseEnrollmentCount(event.courseId());
    }
}

