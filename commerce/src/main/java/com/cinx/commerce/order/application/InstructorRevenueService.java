package com.cinx.commerce.order.application;

import com.cinx.commerce.order.application.query.InstructorRevenue;
import com.cinx.commerce.order.application.query.InstructorRevenueQueries;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gross is each line's amount after the voucher; the fee uses the percent snapshotted on the line and is rounded
 * HALF_UP to whole đồng; net is gross minus fee. Only PAID orders count, dated by when they were paid.
 */
@Service
@RequiredArgsConstructor
public class InstructorRevenueService implements IInstructorRevenueService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final long MAX_DAYS = 400;

    private final InstructorRevenueQueries revenueQueries;
    private final Clock clock;

    @Value("${cinx.commerce.platform-fee-percent:30}")
    private BigDecimal platformFeePercent;

    @Override
    @Transactional(readOnly = true)
    public InstructorRevenue revenue(String instructorId, LocalDate from, LocalDate to, String courseId) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "from and to are required and from must not be after to");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "The range can be at most " + MAX_DAYS + " days");
        }
        String course = courseId == null || courseId.isBlank() || "all".equals(courseId) ? null : courseId;
        LocalDateTime start = wallTime(from);
        LocalDateTime end = wallTime(to.plusDays(1));
        LocalDateTime previousStart = wallTime(from.minusDays(days));

        Map<LocalDate, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDay.put(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        Map<String, CourseTotals> byCourse = new LinkedHashMap<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal previousGross = BigDecimal.ZERO;
        Set<String> orders = new HashSet<>();

        for (InstructorRevenueQueries.RevenueLine line : revenueQueries.findPaidLines(instructorId, previousStart, end, course)) {
            BigDecimal lineGross = line.allocatedAmount() != null ? line.allocatedAmount() : line.priceSnapshot();
            if (line.paidAt().isBefore(start)) {
                previousGross = previousGross.add(lineGross);
                continue;
            }
            BigDecimal percent = line.feePercent() != null ? line.feePercent() : platformFeePercent;
            BigDecimal lineFee = feeOf(lineGross, percent);
            BigDecimal lineNet = lineGross.subtract(lineFee);
            gross = gross.add(lineGross);
            fee = fee.add(lineFee);
            orders.add(line.orderId());

            LocalDate day = line.paidAt().atZone(clock.getZone()).withZoneSameInstant(BUSINESS_ZONE).toLocalDate();
            BigDecimal[] totals = byDay.get(day);
            if (totals != null) {
                totals[0] = totals[0].add(lineGross);
                totals[1] = totals[1].add(lineNet);
            }
            byCourse.computeIfAbsent(line.courseId(), id -> new CourseTotals(line.title())).add(lineGross, lineNet, line.orderId());
        }

        List<InstructorRevenue.Day> perDay = byDay.entrySet().stream()
                .map(e -> new InstructorRevenue.Day(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
        List<InstructorRevenue.Course> perCourse = byCourse.entrySet().stream()
                .map(e -> new InstructorRevenue.Course(e.getKey(), e.getValue().title, e.getValue().gross,
                        e.getValue().net, e.getValue().orders.size()))
                .sorted((a, b) -> b.gross().compareTo(a.gross()))
                .toList();
        return new InstructorRevenue(gross, gross.subtract(fee), fee, platformFeePercent, previousGross, orders.size(), perDay, perCourse);
    }

    private static final class CourseTotals {
        private final String title;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private final Set<String> orders = new HashSet<>();

        private CourseTotals(String title) {
            this.title = title;
        }

        private void add(BigDecimal lineGross, BigDecimal lineNet, String orderId) {
            gross = gross.add(lineGross);
            net = net.add(lineNet);
            orders.add(orderId);
        }
    }

    static BigDecimal feeOf(BigDecimal gross, BigDecimal percent) {
        return gross.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    /** Midnight of a business day, as the wall time of the commerce clock's zone (the zone paidAt is written in). */
    private LocalDateTime wallTime(LocalDate day) {
        return day.atStartOfDay(BUSINESS_ZONE).withZoneSameInstant(clock.getZone()).toLocalDateTime();
    }
}
