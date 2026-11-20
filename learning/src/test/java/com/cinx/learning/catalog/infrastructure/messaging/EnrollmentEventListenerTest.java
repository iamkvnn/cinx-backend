package com.cinx.learning.catalog.infrastructure.messaging;

import com.cinx.learning.catalog.domain.CourseEnrollmentEventRepository;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.infrastructure.messaging.event.LearningEnrollmentGranted;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentEventListenerTest {
    @Test
    void duplicateEnrollmentEventDoesNotIncreaseEnrollmentCount() {
        CourseEnrollmentEventRepository events = mock(CourseEnrollmentEventRepository.class);
        ICourseService courses = mock(ICourseService.class);
        EnrollmentEventListener listener = new EnrollmentEventListener(events, courses);
        when(events.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        listener.onEnrollmentGranted(new LearningEnrollmentGranted("user-1", "course-1", "2026-07-10T00:00:00"));

        verify(courses, never()).increaseEnrollmentCount("course-1");
    }

    @Test
    void newEnrollmentEventIncreasesEnrollmentCountAfterDedupInsert() {
        CourseEnrollmentEventRepository events = mock(CourseEnrollmentEventRepository.class);
        ICourseService courses = mock(ICourseService.class);
        EnrollmentEventListener listener = new EnrollmentEventListener(events, courses);
        when(events.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onEnrollmentGranted(new LearningEnrollmentGranted("user-1", "course-1", "2026-07-10T00:00:00"));

        verify(courses).increaseEnrollmentCount("course-1");
    }
}
