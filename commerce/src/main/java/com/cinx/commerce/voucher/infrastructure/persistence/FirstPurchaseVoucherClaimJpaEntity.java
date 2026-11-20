package com.cinx.commerce.voucher.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A learner's single first-purchase voucher claim, keyed by the learner. Written with its redemption, never on its own. */
@Entity
@Table(name = "first_purchase_voucher_claim")
@Getter
@Setter
@NoArgsConstructor
public class FirstPurchaseVoucherClaimJpaEntity {
    @Id
    private String userId;
    @Column(name = "redemption_id", nullable = false)
    private String redemptionId;
    @Column(nullable = false)
    private LocalDateTime claimedAt;
}
