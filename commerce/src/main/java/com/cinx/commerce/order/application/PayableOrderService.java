package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayableOrderService implements PayableOrders {
    private final OrderRepository orders;
    private final OrderPaidPublisher paidEvents;

    @Override
    @Transactional
    public Optional<PayableOrder> lockForUser(String userId, String orderId) {
        return orders.lockById(orderId).filter(found -> found.getUserId().equals(userId)).map(PayableOrder::of);
    }

    @Override
    @Transactional
    public Optional<PayableOrder> lock(String orderId) {
        return orders.lockById(orderId).map(PayableOrder::of);
    }

    @Override
    @Transactional
    public void markPaid(String orderId, LocalDateTime now) {
        CommerceOrder order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        order.markPaid(now);
        paidEvents.publish(orders.save(order));
    }
}
