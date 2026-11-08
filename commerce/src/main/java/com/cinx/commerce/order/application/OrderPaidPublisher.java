package com.cinx.commerce.order.application;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.shared.application.DomainEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Records the CommerceOrderPaid event, built the same way wherever an order becomes PAID (a provider callback, or
 * checkout of a 0đ order). Call it in the transaction that marks the order paid.
 */
@Component
@RequiredArgsConstructor
class OrderPaidPublisher {
    private final DomainEvents events;

    void publish(CommerceOrder order) {
        events.publish("CommerceOrderPaid", order.getId(), Map.of(
                "orderId", order.getId(), "userId", order.getUserId(), "totalAmount", order.getTotalAmount(),
                "originalAmount", order.getOriginalAmount(), "discountAmount", order.getDiscountAmount(),
                "items", order.getItems().stream().map(item -> Map.of(
                        "courseId", item.getCourseId(), "title", item.getTitleSnapshot(), "price", item.getPriceSnapshot())).toList()));
    }
}
