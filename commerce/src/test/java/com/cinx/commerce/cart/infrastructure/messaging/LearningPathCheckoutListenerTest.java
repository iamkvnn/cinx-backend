package com.cinx.commerce.cart.infrastructure.messaging;

import com.cinx.commerce.cart.domain.CartItem;
import com.cinx.commerce.cart.domain.CartRepository;
import com.cinx.commerce.cart.infrastructure.messaging.event.LearningPathCheckoutRequested;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.shared.infrastructure.messaging.InboxMessageRepository;
import com.cinx.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LearningPathCheckoutListenerTest {
    InboxMessageRepository inbox = mock(InboxMessageRepository.class);
    CartRepository cart = mock(CartRepository.class);
    SellableCourses courses = mock(SellableCourses.class);
    LearningPathCheckoutListener listener = new LearningPathCheckoutListener(inbox, cart, courses);

    @Test
    void duplicateInboxInsertDoesNotMutateCart() {
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate event"));

        listener.consume(new LearningPathCheckoutRequested("event-1", "user-1", "course-1"));

        verifyNoInteractions(cart, courses);
    }

    @Test
    void aSellableCourseOfTheLearningPathIsPutInTheCart() {
        when(courses.findSellable("course-1")).thenReturn(Optional.of(
                new SellableCourseView("course-1", "Course", new BigDecimal("10.00"), null, null, true)));
        when(cart.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(false);

        listener.consume(new LearningPathCheckoutRequested("event-1", "user-1", "course-1"));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cart).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getCourseId()).isEqualTo("course-1");
    }

    @Test
    void aCourseAlreadyInTheCartIsNotAddedAgain() {
        when(courses.findSellable("course-1")).thenReturn(Optional.of(
                new SellableCourseView("course-1", "Course", new BigDecimal("10.00"), null, null, true)));
        when(cart.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(true);

        listener.consume(new LearningPathCheckoutRequested("event-1", "user-1", "course-1"));

        verify(cart, never()).save(any());
    }

    @Test
    void aCourseThatIsNotOnSaleFailsTheEventSoItIsRetriedOrDeadLettered() {
        when(courses.findSellable("course-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.consume(new LearningPathCheckoutRequested("event-1", "user-1", "course-1")))
                .isInstanceOf(BadRequestException.class);
        verify(cart, never()).save(any());
    }
}
