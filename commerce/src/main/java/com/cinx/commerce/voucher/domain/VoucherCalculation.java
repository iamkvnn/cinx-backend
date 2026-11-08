package com.cinx.commerce.voucher.domain;

import java.math.BigDecimal;

public record VoucherCalculation(BigDecimal eligibleSubtotal, int eligibleItems, BigDecimal discountAmount) { }
