package com.cinx.commerce.voucher.api.dto;

import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import jakarta.validation.constraints.*;

public record VoucherTargetRuleRequest(@NotNull TargetType targetType, @NotNull MatchType matchType,
                                       @NotBlank String targetId) { }
