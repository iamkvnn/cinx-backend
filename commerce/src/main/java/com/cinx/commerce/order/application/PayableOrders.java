package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.query.PayableOrder;

import java.time.LocalDateTime;
import java.util.Optional;

/** What the payment feature may ask of the orders: lock one to collect money for it, and mark it paid. */
public interface PayableOrders {
    /** The learner's own order, locked until the surrounding transaction ends; another learner's order is not found. */
    Optional<PayableOrder> lockForUser(String userId, String orderId);

    /** Any order, locked until the surrounding transaction ends. */
    Optional<PayableOrder> lock(String orderId);

    /** Marks the order paid and records the event that grants the courses, in the caller's transaction. */
    void markPaid(String orderId, LocalDateTime now);
}
