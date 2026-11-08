package com.cinx.commerce.voucher.api.dto;

import com.cinx.commerce.shared.domain.DiscountType;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Set;

public record VoucherResponse(String id, String code, DiscountType discountType, BigDecimal discountValue,
        BigDecimal maxDiscountAmount, BigDecimal minimumEligibleAmount, int minimumEligibleItems,
        long totalUsageLimit, long usedCount, long perUserLimit, boolean firstPurchaseOnly,
        LocalDateTime validFrom, LocalDateTime validTo, boolean active,
        Set<VoucherTargetRuleRequest> targetRules) { }
