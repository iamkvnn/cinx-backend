package com.cinx.commerce.catalog.infrastructure.messaging;

import com.cinx.commerce.catalog.infrastructure.messaging.event.CourseSellabilityChanged;
import com.cinx.commerce.catalog.domain.SellableCourseRepository;

import com.cinx.commerce.shared.infrastructure.messaging.InboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CourseSellabilityListenerTest {
    @Test
    void duplicateInboxInsertDoesNotUpdateProjection() {
        SellableCourseRepository courses = mock(SellableCourseRepository.class);
        InboxMessageRepository inbox = mock(InboxMessageRepository.class);
        CourseSellabilityListener listener = new CourseSellabilityListener(courses, inbox);
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate event"));

        listener.consume(new CourseSellabilityChanged(
                "event-1", "course-1", "Course", new BigDecimal("100.00"), "category-1", "instructor-1",
                true, LocalDateTime.now()));

        verifyNoInteractions(courses);
    }
}
