package com.cinx.commerce.voucher.api.dto;

import com.cinx.commerce.shared.domain.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.math.BigDecimal;

public record CreateVoucherRequest(@NotBlank @Size(max = 120) String code, @NotNull DiscountType discountType,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal discountValue,
        @Positive @Digits(integer = 17, fraction = 2) BigDecimal maxDiscountAmount,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal minimumEligibleAmount, @PositiveOrZero int minimumEligibleItems,
        @Positive long totalUsageLimit, @Positive long perUserLimit, boolean firstPurchaseOnly,
        @NotNull LocalDateTime validFrom, @NotNull LocalDateTime validTo,
        @Valid Set<VoucherTargetRuleRequest> targetRules) { }
