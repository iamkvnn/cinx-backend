package com.cinx.commerce.order.domain;

import com.cinx.commerce.shared.domain.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A learner's order for the courses of a cart: priced and discounted when placed, then paid or cancelled. Its lines are
 * part of it. The voucher is only a snapshot (code, type, value); the voucher itself belongs to the voucher feature.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceOrder {
    private String id;
    private String userId;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String voucherCode;
    private DiscountType voucherDiscountType;
    private BigDecimal voucherDiscountValue;
    private String idempotencyKey;
    private String requestFingerprint;
    private OrderStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    @Builder.Default
    private List<CommerceOrderItem> items = new ArrayList<>();

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    /** Whether nothing is left to pay, so the order is settled the moment it is placed. */
    public boolean isFree() {
        return totalAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    public void markPaid(LocalDateTime now) {
        status = OrderStatus.PAID;
        paidAt = now;
    }

    public void cancel(LocalDateTime now) {
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
    }

    /** Records which voucher the discount came from; the order keeps the terms it was sold under. */
    public void snapshotVoucher(String code, DiscountType type, BigDecimal value) {
        voucherCode = code;
        voucherDiscountType = type;
        voucherDiscountValue = value;
    }
}
