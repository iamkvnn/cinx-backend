package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.domain.DiscountType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_order")
@Getter
@Setter
@NoArgsConstructor
public class CommerceOrderJpaEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    private String voucherCode;
    @Enumerated(EnumType.STRING)
    private DiscountType voucherDiscountType;
    @Column(precision = 19, scale = 2)
    private BigDecimal voucherDiscountValue;
    @Column(nullable = false)
    private String idempotencyKey;
    @Column(nullable = false)
    private String requestFingerprint;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommerceOrderItemJpaEntity> items = new ArrayList<>();
}
