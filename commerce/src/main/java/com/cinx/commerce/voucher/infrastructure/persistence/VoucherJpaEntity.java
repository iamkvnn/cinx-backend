package com.cinx.commerce.voucher.infrastructure.persistence;

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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor
public class VoucherJpaEntity {
    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;
    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumEligibleAmount;
    @Column(nullable = false)
    private int minimumEligibleItems;
    @Column(nullable = false)
    private long totalUsageLimit;
    @Column(nullable = false)
    private long usedCount;
    @Column(nullable = false)
    private long perUserLimit;
    @Column(nullable = false)
    private boolean firstPurchaseOnly;
    @Column(nullable = false)
    private LocalDateTime validFrom;
    @Column(nullable = false)
    private LocalDateTime validTo;
    @Column(nullable = false)
    private boolean active;
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VoucherTargetRuleJpaEntity> targetRules = new HashSet<>();
}
