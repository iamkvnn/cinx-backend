package com.cinx.commerce.voucher.application;

import com.cinx.commerce.shared.domain.DiscountType;

import java.math.BigDecimal;
import java.util.Set;

/**
 * A voucher that checked out against a cart but is not consumed yet: the terms to snapshot on the order, what it takes
 * off, and the courses the discount is split across.
 */
public record AppliedVoucher(String voucherId, String code, DiscountType discountType, BigDecimal discountValue,
                             BigDecimal eligibleSubtotal, BigDecimal discountAmount, Set<String> discountedCourseIds,
                             boolean firstPurchaseOnly) {
}
