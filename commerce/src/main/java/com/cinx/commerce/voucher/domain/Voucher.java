package com.cinx.commerce.voucher.domain;

import com.cinx.commerce.shared.domain.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A discount code: what it takes off, how often and by whom it may be used, when it is valid and which courses it covers.
 * The target rules are part of it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    private String id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumEligibleAmount;
    private int minimumEligibleItems;
    private long totalUsageLimit;
    private long usedCount;
    private long perUserLimit;
    private boolean firstPurchaseOnly;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean active;
    @Builder.Default
    private Set<VoucherTargetRule> targetRules = new HashSet<>();

    /** One more use of the voucher has been consumed by an order. */
    public void recordUse() {
        usedCount++;
    }

    /** Swaps every target rule for the given ones. */
    public void replaceTargetRules(Set<VoucherTargetRule> replacement) {
        targetRules = new HashSet<>(replacement);
    }
}
