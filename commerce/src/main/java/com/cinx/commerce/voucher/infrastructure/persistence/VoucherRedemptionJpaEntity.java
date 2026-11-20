package com.cinx.commerce.voucher.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The voucher and the order are held by id; the foreign keys are enforced by the schema. */
@Entity
@Table(name = "voucher_redemption")
@Getter
@Setter
@NoArgsConstructor
public class VoucherRedemptionJpaEntity {
    @Id
    private String id;
    @Column(name = "voucher_id", nullable = false)
    private String voucherId;
    @Column(nullable = false)
    private String userId;
    @Column(name = "order_id", nullable = false)
    private String orderId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal eligibleSubtotal;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(nullable = false)
    private LocalDateTime consumedAt;
    @Column(nullable = false)
    private boolean firstPurchaseClaim;
}
