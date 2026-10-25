package com.cinx.learning.catalog.application.event;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CourseSellabilityChanged(
        String eventId, String courseId, String title, BigDecimal price, String categoryId, String instructorId,
        boolean sellable, LocalDateTime occurredAt) {
}
