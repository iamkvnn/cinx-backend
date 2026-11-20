package com.cinx.commerce.order.api.dto;

import com.cinx.commerce.shared.domain.DiscountType;
import java.math.BigDecimal;

public record VoucherSnapshotResponse(String code, DiscountType type, BigDecimal value) { }
