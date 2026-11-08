package com.cinx.commerce.voucher.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One use of a voucher by one learner on one order. It is never undone: cancelling the order leaves the use consumed.
 * A redemption flagged {@code firstPurchaseClaim} also uses up the learner's one first-purchase voucher.
 */
@Getter
@Builder
@AllArgsConstructor
public class VoucherRedemption {
    private final String id;
    private final String voucherId;
    private final String userId;
    private final String orderId;
    private final BigDecimal eligibleSubtotal;
    private final BigDecimal discountAmount;
    private final LocalDateTime consumedAt;
    private final boolean firstPurchaseClaim;
}
