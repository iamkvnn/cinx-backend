package com.cinx.learning.enrollment.infrastructure.messaging.event;

import java.util.List;
import java.math.BigDecimal;

public record CommerceOrderPaid(String eventId, String orderId, String userId, BigDecimal totalAmount,
                                List<Item> items) {
    public record Item(String courseId, String title, BigDecimal price) {
    }
}
