package com.cinx.commerce.voucher.infrastructure.persistence;

import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherRedemption;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherPersistenceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);
    VoucherPersistenceMapper mapper = new VoucherPersistenceMapper();

    @Test
    void aVoucherAndItsRulesRoundTrip() {
        Voucher voucher = voucher(rule("r1", "c1"), rule("r2", "c2"));

        Voucher back = mapper.toDomain(mapper.toEntity(voucher));

        assertThat(back.getCode()).isEqualTo("SAVE10");
        assertThat(back.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(back.getMaxDiscountAmount()).isEqualByComparingTo("5000");
        assertThat(back.isFirstPurchaseOnly()).isTrue();
        assertThat(back.getTargetRules()).extracting(VoucherTargetRule::getId, VoucherTargetRule::getTargetId)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("r1", "c1"), org.assertj.core.groups.Tuple.tuple("r2", "c2"));
    }

    @Test
    void updatingKeepsTheRowsOfRulesThatDidNotChangeRemovesTheGoneOnesAndAddsTheNewOnes() {
        VoucherJpaEntity stored = mapper.toEntity(voucher(rule("r1", "c1"), rule("r2", "c2")));
        VoucherTargetRuleJpaEntity keptRow = stored.getTargetRules().stream().filter(row -> row.getId().equals("r1")).findFirst().orElseThrow();
        Voucher changed = mapper.toDomain(stored);
        changed.replaceTargetRules(Set.of(rule("r1", "c1"), rule("r3", "c3")));

        mapper.update(stored, changed);

        assertThat(stored.getTargetRules()).extracting(VoucherTargetRuleJpaEntity::getId).containsExactlyInAnyOrder("r1", "r3");
        assertThat(stored.getTargetRules()).contains(keptRow); // the same row, not a rewritten copy
        assertThat(stored.getTargetRules()).allSatisfy(row -> assertThat(row.getVoucher()).isSameAs(stored));
    }

    @Test
    void theUseCountAndTheActiveFlagAreWrittenOntoTheStoredRow() {
        VoucherJpaEntity stored = mapper.toEntity(voucher());
        Voucher changed = mapper.toDomain(stored);
        changed.recordUse();
        changed.setActive(false);

        mapper.update(stored, changed);

        assertThat(stored.getUsedCount()).isEqualTo(1);
        assertThat(stored.isActive()).isFalse();
    }

    @Test
    void thePageIsOneBasedInTheApplicationAndZeroBasedInSpringData(@Mock VoucherJpaRepository jpa) {
        VoucherRepositoryAdapter adapter = new VoucherRepositoryAdapter(jpa, mapper);
        when(jpa.findAll(PageRequest.of(1, 5))).thenReturn(new PageImpl<>(List.of(mapper.toEntity(voucher())), PageRequest.of(1, 5), 11));

        PageResult<Voucher> page = adapter.findAll(new PageQuery(2, 5, null));

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).hasSize(1);
    }

    @Test
    void aFirstPurchaseRedemptionAlsoWritesTheLearnersClaimAndAPlainOneDoesNot(
            @Mock VoucherRedemptionJpaRepository redemptions, @Mock FirstPurchaseVoucherClaimJpaRepository claims) {
        VoucherRedemptionRepositoryAdapter adapter = new VoucherRedemptionRepositoryAdapter(redemptions, claims);

        adapter.save(redemption("red-1", false));
        verify(claims, never()).save(any());

        adapter.save(redemption("red-2", true));
        ArgumentCaptor<FirstPurchaseVoucherClaimJpaEntity> claim = ArgumentCaptor.forClass(FirstPurchaseVoucherClaimJpaEntity.class);
        verify(claims).save(claim.capture());
        assertThat(claim.getValue().getUserId()).isEqualTo("user-1");
        assertThat(claim.getValue().getRedemptionId()).isEqualTo("red-2");
        assertThat(claim.getValue().getClaimedAt()).isEqualTo(NOW);
        verify(redemptions, times(2)).saveAndFlush(any(VoucherRedemptionJpaEntity.class));
    }

    private static VoucherRedemption redemption(String id, boolean claim) {
        return VoucherRedemption.builder().id(id).voucherId("v1").userId("user-1").orderId("o1").eligibleSubtotal(new BigDecimal("100"))
                .discountAmount(new BigDecimal("10")).consumedAt(NOW).firstPurchaseClaim(claim).build();
    }

    private static VoucherTargetRule rule(String id, String target) {
        return VoucherTargetRule.builder().id(id).targetType(TargetType.COURSE).matchType(MatchType.ALLOW).targetId(target).build();
    }

    private static Voucher voucher(VoucherTargetRule... rules) {
        Voucher voucher = Voucher.builder().id("v1").code("SAVE10").discountType(DiscountType.PERCENT).discountValue(new BigDecimal("10"))
                .maxDiscountAmount(new BigDecimal("5000")).minimumEligibleAmount(BigDecimal.ZERO).minimumEligibleItems(0)
                .totalUsageLimit(100).usedCount(0).perUserLimit(1).firstPurchaseOnly(true).validFrom(NOW.minusDays(1))
                .validTo(NOW.plusDays(1)).active(true).build();
        voucher.replaceTargetRules(Set.of(rules));
        return voucher;
    }
}
