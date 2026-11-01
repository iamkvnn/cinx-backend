package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.domain.LearningActivityGroupBy;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LearningActivityRangeResolver {
    static final int DEFAULT_MONTH_BUCKETS = 12;
    static final int MAX_MONTH_BUCKETS = 12;
    static final int DEFAULT_DAY_BUCKETS = 30;
    static final int MAX_DAY_BUCKETS = 30;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public LearningActivityDateRange resolve(LearningActivityGroupBy groupBy, LocalDate startDate, LocalDate endDate) {
        LearningActivityGroupBy normalizedGroupBy = groupBy != null ? groupBy : LearningActivityGroupBy.MONTH;
        if (startDate == null && endDate == null) {
            return normalizedGroupBy == LearningActivityGroupBy.DAY
                    ? resolveLastDays(DEFAULT_DAY_BUCKETS)
                    : resolveLastMonths(DEFAULT_MONTH_BUCKETS);
        }
        if (startDate == null || endDate == null) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "Start date and end date are required together");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "Start date cannot be after end date");
        }
        return normalizedGroupBy == LearningActivityGroupBy.DAY
                ? resolveDaily(startDate, endDate)
                : resolveMonthly(startDate, endDate);
    }

    private LearningActivityDateRange resolveLastMonths(int months) {
        if (months <= 0) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "Months must be greater than 0");
        }
        if (months > MAX_MONTH_BUCKETS) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "Monthly statistics cannot exceed 12 months");
        }
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months - 1L);
        return monthlyRange(startMonth, endMonth);
    }

    private LearningActivityDateRange resolveLastDays(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        return dailyRange(startDate, endDate);
    }

    private LearningActivityDateRange resolveMonthly(LocalDate startDate, LocalDate endDate) {
        return monthlyRange(YearMonth.from(startDate), YearMonth.from(endDate));
    }

    private LearningActivityDateRange resolveDaily(LocalDate startDate, LocalDate endDate) {
        long dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        if (dayCount > MAX_DAY_BUCKETS) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "Daily statistics cannot exceed 30 days");
        }
        validateDailyLookback(startDate);
        return dailyRange(startDate, endDate);
    }

    private LearningActivityDateRange monthlyRange(YearMonth startMonth, YearMonth endMonth) {
        if (startMonth.isAfter(endMonth)) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "Start date cannot be after end date");
        }
        long monthCount = ChronoUnit.MONTHS.between(startMonth, endMonth) + 1L;
        if (monthCount > MAX_MONTH_BUCKETS) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "Monthly statistics cannot exceed 12 months");
        }
        validateMonthlyLookback(startMonth);

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < monthCount; i++) {
            labels.add(startMonth.plusMonths(i).format(MONTH_FORMATTER));
        }
        return new LearningActivityDateRange(startMonth.atDay(1), endMonth.atEndOfMonth(), LearningActivityGroupBy.MONTH, labels);
    }

    private LearningActivityDateRange dailyRange(LocalDate startDate, LocalDate endDate) {
        long dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            labels.add(startDate.plusDays(i).format(DAY_FORMATTER));
        }
        return new LearningActivityDateRange(startDate, endDate, LearningActivityGroupBy.DAY, labels);
    }

    private void validateMonthlyLookback(YearMonth startMonth) {
        YearMonth earliestMonth = YearMonth.now().minusMonths(MAX_MONTH_BUCKETS - 1L);
        if (startMonth.isBefore(earliestMonth)) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "Statistics cannot look back more than 12 months");
        }
    }

    private void validateDailyLookback(LocalDate startDate) {
        LocalDate earliestDate = LocalDate.now().minusMonths(MAX_MONTH_BUCKETS);
        if (startDate.isBefore(earliestDate)) {
            throw new BadRequestException(ErrorCode.STATISTICS_RANGE_TOO_LARGE, "Statistics cannot look back more than 12 months");
        }
    }
}
