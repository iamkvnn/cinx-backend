package com.cinx.commerce.order.api.dto;

import com.cinx.commerce.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public record OrderResponse(String id, BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal totalAmount,
        VoucherSnapshotResponse voucher, OrderStatus status, LocalDateTime paidAt, List<OrderItemResponse> items,
        LocalDateTime createdAt, LocalDateTime cancelledAt) { }
