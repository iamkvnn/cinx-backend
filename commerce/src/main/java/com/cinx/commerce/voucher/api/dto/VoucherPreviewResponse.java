package com.cinx.commerce.voucher.api.dto;

import java.math.BigDecimal;

public record VoucherPreviewResponse(String code, BigDecimal eligibleSubtotal, BigDecimal discountAmount, BigDecimal finalAmount) { }
