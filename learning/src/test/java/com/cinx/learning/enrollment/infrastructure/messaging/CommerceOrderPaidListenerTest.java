package com.cinx.learning.enrollment.infrastructure.messaging;

import static org.mockito.Mockito.*;

import com.cinx.learning.enrollment.infrastructure.messaging.event.CommerceOrderPaid;
import com.cinx.learning.shared.infrastructure.messaging.InboxMessageRepository;
import com.cinx.learning.enrollment.application.EntitlementService;
import com.cinx.learning.learningpath.application.ILearningPathService;
import com.cinx.learning.progress.application.ILearningProgressService;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CommerceOrderPaidListenerTest {
    @Test
    void replayDoesNotDuplicateEntitlementOrProgress() {
        InboxMessageRepository inbox = mock(InboxMessageRepository.class);
        EntitlementService entitlements = mock(EntitlementService.class);
        ILearningProgressService progress = mock(ILearningProgressService.class);
        ILearningPathService paths = mock(ILearningPathService.class);
        CommerceOrderPaidListener listener = new CommerceOrderPaidListener(inbox, entitlements, progress, paths);
        CommerceOrderPaid event = new CommerceOrderPaid("event-1", "order-1", "user-1", new BigDecimal("100.00"),
                List.of(new CommerceOrderPaid.Item("course-1", "Course", new BigDecimal("100.00"))));
        when(inbox.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entitlements.grant("order-1", "user-1", "course-1")).thenReturn(true);

        listener.consume(event);

        verify(entitlements, times(1)).grant("order-1", "user-1", "course-1");
        verify(progress, times(1)).createCourseProgress("user-1", "course-1");
        verify(paths, times(1)).activatePendingPathForCourse("user-1", "course-1");
        verify(inbox, times(1)).saveAndFlush(any());
    }

    @Test
    void duplicateInboxInsertDoesNotMutateState() {
        InboxMessageRepository inbox = mock(InboxMessageRepository.class);
        EntitlementService entitlements = mock(EntitlementService.class);
        ILearningProgressService progress = mock(ILearningProgressService.class);
        ILearningPathService paths = mock(ILearningPathService.class);
        CommerceOrderPaidListener listener = new CommerceOrderPaidListener(inbox, entitlements, progress, paths);
        CommerceOrderPaid event = new CommerceOrderPaid("event-1", "order-1", "user-1", new BigDecimal("100.00"),
                List.of(new CommerceOrderPaid.Item("course-1", "Course", new BigDecimal("100.00"))));
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate event"));

        listener.consume(event);

        verifyNoInteractions(entitlements, progress, paths);
    }
}
