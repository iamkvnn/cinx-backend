package com.cinx.commerce.order.domain;

import com.cinx.commerce.shared.domain.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommerceOrderTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Test
    void paidAndCancelledRecordWhenAndChangeTheStatus() {
        CommerceOrder paid = order("100.00");
        paid.markPaid(NOW);
        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.isPending()).isFalse();
        assertThat(paid.getPaidAt()).isEqualTo(NOW);

        CommerceOrder cancelled = order("100.00");
        cancelled.cancel(NOW);
        assertThat(cancelled.isCancelled()).isTrue();
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void anOrderIsFreeWhenNothingIsLeftToPay() {
        assertThat(order("0.00").isFree()).isTrue();
        assertThat(order("-5.00").isFree()).isTrue();
        assertThat(order("0.01").isFree()).isFalse();
    }

    @Test
    void theVoucherSnapshotKeepsTheTermsItWasSoldUnder() {
        CommerceOrder order = order("100.00");

        order.snapshotVoucher("SAVE10", DiscountType.PERCENT, new BigDecimal("10"));

        assertThat(order.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(order.getVoucherDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(order.getVoucherDiscountValue()).isEqualByComparingTo("10");
    }

    private static CommerceOrder order(String total) {
        return CommerceOrder.builder().id("o1").totalAmount(new BigDecimal(total)).status(OrderStatus.PENDING).build();
    }
}
