package com.cinx.commerce.voucher.domain;

/** Port: where voucher uses, and the learners' first-purchase claims, are stored. */
public interface VoucherRedemptionRepository {
    long countByVoucherIdAndUserId(String voucherId, String userId);

    boolean existsByVoucherId(String voucherId);

    /** Whether the learner has already used up their one first-purchase voucher. */
    boolean existsFirstPurchaseClaim(String userId);

    /** Stores the use, written out at once, and the first-purchase claim when the use carries one. */
    VoucherRedemption save(VoucherRedemption redemption);
}
