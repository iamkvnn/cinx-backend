package com.cinx.commerce.order.application.query;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** An order as its owner or an administrator reads it. */
public record OrderView(String id, BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal totalAmount,
                        VoucherSnapshot voucher, OrderStatus status, LocalDateTime paidAt, List<Item> items,
                        LocalDateTime createdAt, LocalDateTime cancelledAt) {

    /** The terms of the voucher the order was sold under. */
    public record VoucherSnapshot(String code, DiscountType type, BigDecimal value) {
    }

    public record Item(String courseId, String title, BigDecimal price) {
    }

    public static OrderView of(CommerceOrder order) {
        VoucherSnapshot voucher = order.getVoucherCode() == null ? null
                : new VoucherSnapshot(order.getVoucherCode(), order.getVoucherDiscountType(), order.getVoucherDiscountValue());
        return new OrderView(order.getId(), order.getOriginalAmount(), order.getDiscountAmount(), order.getTotalAmount(), voucher,
                order.getStatus(), order.getPaidAt(),
                order.getItems().stream().map(item -> new Item(item.getCourseId(), item.getTitleSnapshot(), item.getPriceSnapshot())).toList(),
                order.getCreatedAt(), order.getCancelledAt());
    }
}
