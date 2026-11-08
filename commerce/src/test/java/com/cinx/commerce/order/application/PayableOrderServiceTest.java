package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.CommerceOrderItem;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.application.DomainEvents;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayableOrderServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock OrderRepository orders;
    @Mock DomainEvents events;
    PayableOrderService service;

    @BeforeEach
    void setUp() {
        service = new PayableOrderService(orders, new OrderPaidPublisher(events));
    }

    @Test
    void lockingForTheOwnerShowsTheOrderAndForSomeoneElseShowsNothing() {
        when(orders.lockById("o1")).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThat(service.lockForUser("user-1", "o1")).contains(new PayableOrder("o1", new BigDecimal("100.00"), true, false, false));
        assertThat(service.lockForUser("someone-else", "o1")).isEmpty();
    }

    @Test
    void lockingByIdAloneShowsTheStatusFlags() {
        when(orders.lockById("o1")).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));
        when(orders.lockById("gone")).thenReturn(Optional.empty());

        assertThat(service.lock("o1")).contains(new PayableOrder("o1", new BigDecimal("100.00"), false, false, true));
        assertThat(service.lock("gone")).isEmpty();
    }

    @Test
    void markingPaidStoresTheOrderAndRecordsOnePaidEventWithTheLines() {
        CommerceOrder order = order(OrderStatus.PENDING);
        when(orders.findById("o1")).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);

        service.markPaid("o1", NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(events).publish(eq("CommerceOrderPaid"), eq("o1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("orderId", "o1").containsEntry("userId", "user-1");
        assertThat(payload.getValue().get("totalAmount")).isEqualTo(new BigDecimal("100.00"));
        Map<String, Object> expectedLine = Map.of("courseId", "c1", "title", "Title", "price", new BigDecimal("100.00"));
        assertThat((List<Object>) payload.getValue().get("items")).containsExactly(expectedLine);
    }

    @Test
    void markingAnUnknownOrderPaidFailsAndPublishesNothing() {
        when(orders.findById("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid("gone", NOW)).isInstanceOf(NotFoundException.class);
        verify(events, never()).publish(any(), any(), any());
    }

    private static CommerceOrder order(OrderStatus status) {
        return CommerceOrder.builder().id("o1").userId("user-1").originalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("100.00")).status(status)
                .items(List.of(CommerceOrderItem.builder().id("i1").courseId("c1").titleSnapshot("Title")
                        .priceSnapshot(new BigDecimal("100.00")).build())).build();
    }
}
