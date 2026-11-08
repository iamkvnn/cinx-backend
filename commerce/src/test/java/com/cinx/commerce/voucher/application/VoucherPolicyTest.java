package com.cinx.commerce.voucher.application;

import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherCalculation;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;

import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class VoucherPolicyTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 11, 10, 0);
    private final VoucherPolicy policy = new VoucherPolicy(Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()));

    @Test void fixedDiscountIsLimitedToEligibleSubtotal() {
        Voucher voucher = voucher(DiscountType.FIXED, "500", null);
        VoucherCalculation result = policy.calculate(voucher, List.of(course("c1", "cat1", "i1", "300")));
        assertThat(result.discountAmount()).isEqualByComparingTo("300.00");
    }

    @Test void percentDiscountIsRoundedHalfUpAndCapped() {
        Voucher voucher = voucher(DiscountType.PERCENT, "33", "100");
        VoucherCalculation result = policy.calculate(voucher, List.of(course("c1", "cat1", "i1", "1000")));
        assertThat(result.discountAmount()).isEqualByComparingTo("100.00");
    }

    @Test void allowRulesAreAndAcrossTypesAndDenyWins() {
        Voucher voucher = voucher(DiscountType.FIXED, "50", null);
        voucher.getTargetRules().add(rule(voucher, TargetType.CATEGORY, MatchType.ALLOW, "cat1"));
        voucher.getTargetRules().add(rule(voucher, TargetType.INSTRUCTOR, MatchType.ALLOW, "i1"));
        voucher.getTargetRules().add(rule(voucher, TargetType.COURSE, MatchType.DENY, "blocked"));
        VoucherCalculation result = policy.calculate(voucher, List.of(
                course("ok", "cat1", "i1", "200"), course("wrong", "cat2", "i1", "300"),
                course("blocked", "cat1", "i1", "400")));
        assertThat(result.eligibleSubtotal()).isEqualByComparingTo("200.00");
    }

    @Test void validToIsExclusive() {
        Voucher voucher = voucher(DiscountType.FIXED, "50", null);
        voucher.setValidTo(now);
        assertThatThrownBy(() -> policy.calculate(voucher, List.of(course("c", "cat", "i", "100"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test void decimalPercentRoundsHalfUpToTwoPlaces() {
        Voucher voucher = voucher(DiscountType.PERCENT, "12.50", "1000.00");
        VoucherCalculation result = policy.calculate(voucher, List.of(course("c1", "cat1", "i1", "10.05")));
        assertThat(result.discountAmount()).isEqualByComparingTo("1.26");
    }

    private Voucher voucher(DiscountType type, String value, String max) {
        return Voucher.builder().id("v").code("CODE").discountType(type).discountValue(new BigDecimal(value))
                .maxDiscountAmount(max == null ? null : new BigDecimal(max)).minimumEligibleAmount(BigDecimal.ZERO).minimumEligibleItems(0)
                .totalUsageLimit(10).usedCount(0).perUserLimit(1).validFrom(now.minusDays(1))
                .validTo(now.plusDays(1)).active(true).build();
    }
    private SellableCourseView course(String id, String category, String instructor, String price) {
        return new SellableCourseView(id, id, new BigDecimal(price), category, instructor, true);
    }
    private VoucherTargetRule rule(Voucher voucher, TargetType target, MatchType match, String id) {
        return VoucherTargetRule.builder().id(UUID.randomUUID().toString())
                .targetType(target).matchType(match).targetId(id).build();
    }
}
