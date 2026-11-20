package com.cinx.commerce.voucher.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherTest {
    @Test
    void eachUseCountsOnce() {
        Voucher voucher = Voucher.builder().id("v1").usedCount(4).build();

        voucher.recordUse();

        assertThat(voucher.getUsedCount()).isEqualTo(5);
    }

    @Test
    void replacingTheTargetRulesDropsTheOldOnesAndCopiesTheNewSet() {
        VoucherTargetRule old = VoucherTargetRule.builder().id("old").build();
        VoucherTargetRule fresh = VoucherTargetRule.builder().id("fresh").build();
        Voucher voucher = Voucher.builder().id("v1").build();
        voucher.replaceTargetRules(Set.of(old));
        java.util.Set<VoucherTargetRule> replacement = new java.util.HashSet<>(Set.of(fresh));

        voucher.replaceTargetRules(replacement);
        replacement.clear();

        assertThat(voucher.getTargetRules()).containsExactly(fresh);
    }

    @Test
    void aNewVoucherHasNoRules() {
        assertThat(Voucher.builder().build().getTargetRules()).isEmpty();
    }
}
