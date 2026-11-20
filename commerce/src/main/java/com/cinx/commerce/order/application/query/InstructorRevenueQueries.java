package com.cinx.commerce.order.application.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Port: reads the paid order lines behind an instructor's revenue. */
public interface InstructorRevenueQueries {
    /** Paid order lines of an instructor's courses, paid within a window (bounds in the commerce clock's zone). */
    List<RevenueLine> findPaidLines(String instructorId, LocalDateTime start, LocalDateTime end, String courseId);

    record RevenueLine(String courseId, String title, BigDecimal priceSnapshot, BigDecimal allocatedAmount,
                       BigDecimal feePercent, LocalDateTime paidAt, String orderId) {
    }
}
