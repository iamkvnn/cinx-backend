package com.cinx.commerce.voucher.application.query;

import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import com.cinx.commerce.voucher.domain.Voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/** A voucher as the administrators see it. */
public record VoucherView(String id, String code, DiscountType discountType, BigDecimal discountValue,
                          BigDecimal maxDiscountAmount, BigDecimal minimumEligibleAmount, int minimumEligibleItems,
                          long totalUsageLimit, long usedCount, long perUserLimit, boolean firstPurchaseOnly,
                          LocalDateTime validFrom, LocalDateTime validTo, boolean active, Set<TargetRuleView> targetRules) {

    public record TargetRuleView(TargetType targetType, MatchType matchType, String targetId) {
    }

    public static VoucherView of(Voucher voucher) {
        Set<TargetRuleView> rules = voucher.getTargetRules().stream()
                .map(rule -> new TargetRuleView(rule.getTargetType(), rule.getMatchType(), rule.getTargetId()))
                .collect(Collectors.toSet());
        return new VoucherView(voucher.getId(), voucher.getCode(), voucher.getDiscountType(), voucher.getDiscountValue(),
                voucher.getMaxDiscountAmount(), voucher.getMinimumEligibleAmount(), voucher.getMinimumEligibleItems(),
                voucher.getTotalUsageLimit(), voucher.getUsedCount(), voucher.getPerUserLimit(), voucher.isFirstPurchaseOnly(),
                voucher.getValidFrom(), voucher.getValidTo(), voucher.isActive(), rules);
    }
}
