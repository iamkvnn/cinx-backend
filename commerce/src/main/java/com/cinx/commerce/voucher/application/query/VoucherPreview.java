package com.cinx.commerce.voucher.application.query;

import java.math.BigDecimal;

/** What a voucher would take off the learner's current cart. */
public record VoucherPreview(String code, BigDecimal eligibleSubtotal, BigDecimal discountAmount, BigDecimal finalAmount) {
}
