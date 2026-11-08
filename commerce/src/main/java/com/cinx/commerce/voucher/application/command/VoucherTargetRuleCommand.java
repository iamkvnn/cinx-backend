package com.cinx.commerce.voucher.application.command;

import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;

/** One allow or deny rule for a voucher. */
public record VoucherTargetRuleCommand(TargetType targetType, MatchType matchType, String targetId) {
}
