package com.cinx.commerce.voucher.application;

import com.cinx.commerce.catalog.application.query.SellableCourseView;

import java.time.LocalDateTime;
import java.util.List;

/** What the orders may ask the vouchers while a cart is checked out. */
public interface VoucherCheckout {
    /**
     * Locks the voucher and checks it against the learner and the courses in their cart. Nothing is consumed yet.
     *
     * @throws com.cinx.common.exception.NotFoundException when there is no voucher with that code
     * @throws com.cinx.common.exception.BadRequestException when the voucher does not apply
     */
    AppliedVoucher apply(String userId, String code, List<SellableCourseView> courses);

    /** Consumes the use for the order: records the redemption, claims a first purchase and counts the use. */
    void consume(AppliedVoucher applied, String userId, String orderId, LocalDateTime now);
}
