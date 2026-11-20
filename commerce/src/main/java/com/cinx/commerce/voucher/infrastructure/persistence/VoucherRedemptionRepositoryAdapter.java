package com.cinx.commerce.voucher.infrastructure.persistence;

import com.cinx.commerce.voucher.domain.VoucherRedemption;
import com.cinx.commerce.voucher.domain.VoucherRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class VoucherRedemptionRepositoryAdapter implements VoucherRedemptionRepository {
    private final VoucherRedemptionJpaRepository redemptions;
    private final FirstPurchaseVoucherClaimJpaRepository claims;

    @Override
    @Transactional(readOnly = true)
    public long countByVoucherIdAndUserId(String voucherId, String userId) {
        return redemptions.countByVoucherIdAndUserId(voucherId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByVoucherId(String voucherId) {
        return redemptions.existsByVoucherId(voucherId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsFirstPurchaseClaim(String userId) {
        return claims.existsById(userId);
    }

    @Override
    @Transactional
    public VoucherRedemption save(VoucherRedemption redemption) {
        VoucherRedemptionJpaEntity row = new VoucherRedemptionJpaEntity();
        row.setId(redemption.getId());
        row.setVoucherId(redemption.getVoucherId());
        row.setUserId(redemption.getUserId());
        row.setOrderId(redemption.getOrderId());
        row.setEligibleSubtotal(redemption.getEligibleSubtotal());
        row.setDiscountAmount(redemption.getDiscountAmount());
        row.setConsumedAt(redemption.getConsumedAt());
        row.setFirstPurchaseClaim(redemption.isFirstPurchaseClaim());
        redemptions.saveAndFlush(row);
        if (redemption.isFirstPurchaseClaim()) {
            FirstPurchaseVoucherClaimJpaEntity claim = new FirstPurchaseVoucherClaimJpaEntity();
            claim.setUserId(redemption.getUserId());
            claim.setRedemptionId(redemption.getId());
            claim.setClaimedAt(redemption.getConsumedAt());
            claims.save(claim);
        }
        return redemption;
    }
}
