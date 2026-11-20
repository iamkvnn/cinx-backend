package com.cinx.commerce.order.application.query;

import com.cinx.commerce.order.domain.CommerceOrder;

import java.math.BigDecimal;

/** What the payment feature needs to know about an order it is about to collect money for. */
public record PayableOrder(String id, BigDecimal totalAmount, boolean pending, boolean paid, boolean cancelled) {

    public static PayableOrder of(CommerceOrder order) {
        return new PayableOrder(order.getId(), order.getTotalAmount(), order.isPending(), order.isPaid(), order.isCancelled());
    }
}
