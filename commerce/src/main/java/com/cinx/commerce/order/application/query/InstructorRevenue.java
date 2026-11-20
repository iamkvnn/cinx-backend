package com.cinx.commerce.order.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** An instructor's course revenue for a period: gross after vouchers, the platform fee, and what the instructor gets. */
public record InstructorRevenue(
        BigDecimal gross,
        BigDecimal net,
        BigDecimal fee,
        BigDecimal feePercent,
        BigDecimal previousGross,
        long orders,
        List<Day> byDay,
        List<Course> byCourse
) {
    public record Day(LocalDate date, BigDecimal gross, BigDecimal net) {
    }

    public record Course(String courseId, String title, BigDecimal gross, BigDecimal net, long orders) {
    }
}
