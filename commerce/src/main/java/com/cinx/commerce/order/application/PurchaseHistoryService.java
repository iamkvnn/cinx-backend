package com.cinx.commerce.order.application;

import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseHistoryService implements PurchaseHistory {
    private final OrderRepository orders;

    @Override
    @Transactional(readOnly = true)
    public Set<String> paidCourseIds(String userId) {
        return new HashSet<>(orders.findPaidCourseIds(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPaidOrder(String userId) {
        return orders.existsByUserIdAndStatus(userId, OrderStatus.PAID);
    }
}
