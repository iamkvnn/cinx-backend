package com.cinx.commerce.voucher.infrastructure.persistence;

import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voucher_target_rule")
@Getter
@Setter
@NoArgsConstructor
public class VoucherTargetRuleJpaEntity {
    @Id
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id")
    private VoucherJpaEntity voucher;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;
    @Column(nullable = false)
    private String targetId;
}
