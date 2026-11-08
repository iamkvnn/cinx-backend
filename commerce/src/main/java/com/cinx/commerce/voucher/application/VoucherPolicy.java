package com.cinx.commerce.voucher.application;

import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherCalculation;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;

import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VoucherPolicy {
    private final Clock clock;

    public VoucherPolicy(Clock clock) { this.clock = clock; }

    public VoucherCalculation calculate(Voucher voucher, List<SellableCourseView> courses) {
        validateAvailability(voucher);
        List<SellableCourseView> eligible = courses.stream().filter(c -> matches(voucher, c)).toList();
        if (eligible.isEmpty())
            throw new BadRequestException(ErrorCode.VOUCHER_INVALID, "Voucher does not apply to any course in the cart");
        BigDecimal subtotal = eligible.stream().map(SellableCourseView::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (eligible.size() < voucher.getMinimumEligibleItems()
                || subtotal.compareTo(voucher.getMinimumEligibleAmount()) < 0)
            throw new BadRequestException(ErrorCode.VOUCHER_MIN_PURCHASE_NOT_MET, "Voucher minimum eligible purchase is not met");
        BigDecimal discount = voucher.getDiscountType() == DiscountType.FIXED
                ? voucher.getDiscountValue().min(subtotal)
                : subtotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .min(voucher.getMaxDiscountAmount());
        return new VoucherCalculation(subtotal, eligible.size(), discount);
    }

    /** Whether the voucher's target rules cover this course (the lines its discount is split across). */
    public boolean appliesTo(Voucher voucher, SellableCourseView course) {
        return matches(voucher, course);
    }

    public void validateAvailability(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!voucher.isActive()) throw new BadRequestException(ErrorCode.VOUCHER_NOT_ACTIVE, "Voucher is not active");
        if (now.isBefore(voucher.getValidFrom()))
            throw new BadRequestException(ErrorCode.VOUCHER_NOT_ACTIVE, "Voucher is not valid yet");
        if (!now.isBefore(voucher.getValidTo()))
            throw new BadRequestException(ErrorCode.VOUCHER_EXPIRED, "Voucher has expired");
        if (voucher.getUsedCount() >= voucher.getTotalUsageLimit())
            throw new BadRequestException(ErrorCode.VOUCHER_OUT_OF_STOCK, "Voucher usage limit has been reached");
    }

    private boolean matches(Voucher voucher, SellableCourseView course) {
        Map<TargetType, List<VoucherTargetRule>> byType = voucher.getTargetRules().stream()
                .collect(Collectors.groupingBy(VoucherTargetRule::getTargetType));
        for (TargetType type : TargetType.values()) {
            List<VoucherTargetRule> rules = byType.getOrDefault(type, List.of());
            String value = switch (type) {
                case COURSE -> course.courseId(); case CATEGORY -> course.categoryId();
                case INSTRUCTOR -> course.instructorId();
            };
            if (rules.stream().anyMatch(r -> r.getMatchType() == MatchType.DENY && Objects.equals(r.getTargetId(), value))) return false;
            List<VoucherTargetRule> allow = rules.stream().filter(r -> r.getMatchType() == MatchType.ALLOW).toList();
            if (!allow.isEmpty() && allow.stream().noneMatch(r -> Objects.equals(r.getTargetId(), value))) return false;
        }
        return true;
    }
}
