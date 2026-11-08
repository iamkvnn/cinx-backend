package com.cinx.commerce.voucher.application.command;

import com.cinx.commerce.shared.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/** The new terms of a voucher that has not been redeemed yet; the code never changes. */
public record UpdateVoucherCommand(DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount,
                                   BigDecimal minimumEligibleAmount, int minimumEligibleItems, long totalUsageLimit,
                                   long perUserLimit, boolean firstPurchaseOnly, LocalDateTime validFrom,
                                   LocalDateTime validTo, Set<VoucherTargetRuleCommand> targetRules) {
}
