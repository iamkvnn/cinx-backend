package com.cinx.commerce.order.application;

import com.cinx.commerce.order.domain.CommerceOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Splits an order's discount across the lines it applied to, in proportion to their list price, rounded HALF_UP to
 * the cent; the last eligible line takes the rounding remainder so the shares add up to the discount exactly.
 */
public final class OrderLineShares {
    private OrderLineShares() {
    }

    static void allocate(List<CommerceOrderItem> items, Set<String> eligibleCourseIds, BigDecimal discount, BigDecimal feePercent) {
        List<CommerceOrderItem> eligible = items.stream().filter(i -> eligibleCourseIds.contains(i.getCourseId())).toList();
        BigDecimal subtotal = eligible.stream().map(CommerceOrderItem::getPriceSnapshot).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = discount == null ? BigDecimal.ZERO : discount;
        for (CommerceOrderItem item : items) {
            item.setPlatformFeePercent(feePercent);
            item.setAllocatedAmount(item.getPriceSnapshot());
        }
        if (remaining.signum() <= 0 || subtotal.signum() <= 0) {
            return;
        }
        BigDecimal total = remaining;
        for (int i = 0; i < eligible.size(); i++) {
            CommerceOrderItem item = eligible.get(i);
            BigDecimal share = i == eligible.size() - 1
                    ? remaining
                    : total.multiply(item.getPriceSnapshot()).divide(subtotal, 2, RoundingMode.HALF_UP);
            share = share.min(item.getPriceSnapshot()).min(remaining);
            item.setAllocatedAmount(item.getPriceSnapshot().subtract(share));
            remaining = remaining.subtract(share);
        }
    }
}
