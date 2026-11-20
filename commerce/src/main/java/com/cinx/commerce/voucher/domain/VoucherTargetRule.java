package com.cinx.commerce.voucher.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** One allow or deny rule of a voucher: a course, a category or an instructor. */
@Getter
@Builder
@AllArgsConstructor
public class VoucherTargetRule {
    private final String id;
    private final TargetType targetType;
    private final MatchType matchType;
    private final String targetId;
}
