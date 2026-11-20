package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.CommerceOrderItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Maps an order and its lines in both directions; written by hand because the lines are a child collection. */
@Component
class OrderPersistenceMapper {

    CommerceOrder toDomain(CommerceOrderJpaEntity entity) {
        List<CommerceOrderItem> items = new ArrayList<>();
        entity.getItems().forEach(item -> items.add(CommerceOrderItem.builder().id(item.getId()).courseId(item.getCourseId())
                .titleSnapshot(item.getTitleSnapshot()).priceSnapshot(item.getPriceSnapshot()).instructorId(item.getInstructorId())
                .allocatedAmount(item.getAllocatedAmount()).platformFeePercent(item.getPlatformFeePercent()).build()));
        return CommerceOrder.builder().id(entity.getId()).userId(entity.getUserId()).originalAmount(entity.getOriginalAmount())
                .discountAmount(entity.getDiscountAmount()).totalAmount(entity.getTotalAmount()).voucherCode(entity.getVoucherCode())
                .voucherDiscountType(entity.getVoucherDiscountType()).voucherDiscountValue(entity.getVoucherDiscountValue())
                .idempotencyKey(entity.getIdempotencyKey()).requestFingerprint(entity.getRequestFingerprint())
                .status(entity.getStatus()).paidAt(entity.getPaidAt()).createdAt(entity.getCreatedAt())
                .cancelledAt(entity.getCancelledAt()).items(items).build();
    }

    CommerceOrderJpaEntity toEntity(CommerceOrder order) {
        CommerceOrderJpaEntity entity = new CommerceOrderJpaEntity();
        entity.setId(order.getId());
        entity.setUserId(order.getUserId());
        entity.setIdempotencyKey(order.getIdempotencyKey());
        entity.setRequestFingerprint(order.getRequestFingerprint());
        entity.setCreatedAt(order.getCreatedAt());
        update(entity, order);
        order.getItems().forEach(item -> {
            CommerceOrderItemJpaEntity row = new CommerceOrderItemJpaEntity();
            row.setId(item.getId());
            row.setOrder(entity);
            row.setCourseId(item.getCourseId());
            row.setTitleSnapshot(item.getTitleSnapshot());
            row.setPriceSnapshot(item.getPriceSnapshot());
            row.setInstructorId(item.getInstructorId());
            row.setAllocatedAmount(item.getAllocatedAmount());
            row.setPlatformFeePercent(item.getPlatformFeePercent());
            entity.getItems().add(row);
        });
        return entity;
    }

    /** Writes what an order can still change after it is placed; its lines are fixed at that point and stay as stored. */
    void update(CommerceOrderJpaEntity entity, CommerceOrder order) {
        entity.setOriginalAmount(order.getOriginalAmount());
        entity.setDiscountAmount(order.getDiscountAmount());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setVoucherCode(order.getVoucherCode());
        entity.setVoucherDiscountType(order.getVoucherDiscountType());
        entity.setVoucherDiscountValue(order.getVoucherDiscountValue());
        entity.setStatus(order.getStatus());
        entity.setPaidAt(order.getPaidAt());
        entity.setCancelledAt(order.getCancelledAt());
    }
}
