package com.cinx.commerce.voucher.application;

import com.cinx.commerce.cart.application.CartContents;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.PurchaseHistory;
import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.application.command.CreateVoucherCommand;
import com.cinx.commerce.voucher.application.command.UpdateVoucherCommand;
import com.cinx.commerce.voucher.application.command.VoucherTargetRuleCommand;
import com.cinx.commerce.voucher.application.query.VoucherPreview;
import com.cinx.commerce.voucher.application.query.VoucherView;
import com.cinx.commerce.voucher.domain.MatchType;
import com.cinx.commerce.voucher.domain.TargetType;
import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherRedemption;
import com.cinx.commerce.voucher.domain.VoucherRedemptionRepository;
import com.cinx.commerce.voucher.domain.VoucherRepository;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock VoucherRepository repository;
    @Mock VoucherRedemptionRepository redemptions;
    @Mock PurchaseHistory purchases;
    @Mock CartContents cart;
    @Mock SellableCourses courses;
    VoucherService service;

    @BeforeEach
    void setUp() {
        service = new VoucherService(repository, redemptions, purchases, cart, courses,
                new VoucherPolicy(Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC)));
    }

    // ---- administration

    @Test
    void aNewVoucherHasANormalizedCodeNoUsesAndIsActive() {
        when(repository.existsByCode("SAVE10")).thenReturn(false);
        when(repository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherView view = service.create(create(" save10 ", DiscountType.FIXED, "10000", null, 100, 1));

        assertThat(view.code()).isEqualTo("SAVE10");
        assertThat(view.usedCount()).isZero();
        assertThat(view.active()).isTrue();
        assertThat(view.targetRules()).containsExactly(new VoucherView.TargetRuleView(TargetType.COURSE, MatchType.ALLOW, "c1"));
    }

    @Test
    void aCodeThatExistsAlreadyIsAConflict() {
        when(repository.existsByCode("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(create("save10", DiscountType.FIXED, "10000", null, 100, 1)))
                .isInstanceOf(AlreadyExistException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void theTermsOfAVoucherAreValidated() {
        // percent needs a ceiling and at most 100, fixed must not have a ceiling, the window must run forward, per-user <= total
        assertThatThrownBy(() -> service.create(create("a", DiscountType.PERCENT, "10", null, 100, 1))).hasMessageContaining("maxDiscountAmount");
        assertThatThrownBy(() -> service.create(create("a", DiscountType.PERCENT, "100.01", "5000", 100, 1))).hasMessageContaining("1..100");
        assertThatThrownBy(() -> service.create(create("a", DiscountType.FIXED, "10000", "5000", 100, 1))).hasMessageContaining("Fixed voucher");
        assertThatThrownBy(() -> service.create(create("a", DiscountType.FIXED, "10000", null, 5, 6))).hasMessageContaining("Per-user");
        CreateVoucherCommand backwards = new CreateVoucherCommand("a", DiscountType.FIXED, BigDecimal.TEN, null, BigDecimal.ZERO, 0, 10, 1, false,
                NOW.plusDays(1), NOW, Set.of());
        assertThatThrownBy(() -> service.create(backwards)).hasMessageContaining("date range");
        verify(repository, never()).save(any());
    }

    @Test
    void aRedeemedVoucherCanOnlyBeDeactivated() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "10000", null);
        when(repository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(redemptions.existsByVoucherId("v1")).thenReturn(true);

        assertThatThrownBy(() -> service.update("save10", update(DiscountType.FIXED, "5000", null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("deactivated");
        verify(repository, never()).save(any());
    }

    @Test
    void updatingAnUnredeemedVoucherReplacesItsTermsAndRules() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "10000", null);
        voucher.replaceTargetRules(Set.of(VoucherTargetRule.builder().id("old").targetType(TargetType.COURSE)
                .matchType(MatchType.ALLOW).targetId("c9").build()));
        when(repository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(redemptions.existsByVoucherId("v1")).thenReturn(false);
        when(repository.save(voucher)).thenReturn(voucher);

        VoucherView view = service.update("save10", update(DiscountType.PERCENT, "20", "30000"));

        assertThat(view.discountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(view.discountValue()).isEqualByComparingTo("20");
        assertThat(view.maxDiscountAmount()).isEqualByComparingTo("30000");
        assertThat(view.targetRules()).containsExactly(new VoucherView.TargetRuleView(TargetType.CATEGORY, MatchType.DENY, "cat1"));
        assertThat(view.code()).isEqualTo("SAVE10");
    }

    @Test
    void deactivatingAVoucherKeepsEverythingElse() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "10000", null);
        when(repository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(repository.save(voucher)).thenReturn(voucher);

        VoucherView view = service.setActive("save10", false);

        assertThat(view.active()).isFalse();
        assertThat(view.discountValue()).isEqualByComparingTo("10000");
    }

    @Test
    void anUnknownVoucherIsNotFound() {
        when(repository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("nope")).isInstanceOf(NotFoundException.class);
    }

    // ---- preview

    @Test
    void previewShowsWhatTheVoucherTakesOffTheCurrentCart() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "30000", null);
        when(repository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(redemptions.countByVoucherIdAndUserId("v1", "user-1")).thenReturn(0L);
        when(cart.courseIdsOf("user-1")).thenReturn(List.of("c1", "c2"));
        when(courses.findSellable("c1")).thenReturn(Optional.of(course("c1", "100000")));
        when(courses.findSellable("c2")).thenReturn(Optional.of(course("c2", "50000")));

        VoucherPreview preview = service.preview("user-1", "save10");

        assertThat(preview.code()).isEqualTo("SAVE10");
        assertThat(preview.eligibleSubtotal()).isEqualByComparingTo("150000");
        assertThat(preview.discountAmount()).isEqualByComparingTo("30000");
        assertThat(preview.finalAmount()).isEqualByComparingTo("120000");
    }

    @Test
    void previewRefusesAVoucherTheLearnerUsedUpOrAFirstPurchaseVoucherAfterABuy() {
        Voucher used = voucher("v1", "USED", DiscountType.FIXED, "1000", null);
        Voucher first = voucher("v2", "FIRST", DiscountType.FIXED, "1000", null);
        first.setFirstPurchaseOnly(true);
        when(repository.findByCode("USED")).thenReturn(Optional.of(used));
        when(repository.findByCode("FIRST")).thenReturn(Optional.of(first));
        when(redemptions.countByVoucherIdAndUserId("v1", "user-1")).thenReturn(1L);
        when(redemptions.countByVoucherIdAndUserId("v2", "user-1")).thenReturn(0L);
        when(redemptions.existsFirstPurchaseClaim("user-1")).thenReturn(false);
        when(purchases.hasPaidOrder("user-1")).thenReturn(true);

        assertThatThrownBy(() -> service.preview("user-1", "used")).hasMessageContaining("per-user limit");
        assertThatThrownBy(() -> service.preview("user-1", "first")).hasMessageContaining("first purchase");
    }

    @Test
    void previewNeedsAnItemInTheCart() {
        when(repository.findByCode("SAVE10")).thenReturn(Optional.of(voucher("v1", "SAVE10", DiscountType.FIXED, "1000", null)));
        when(cart.courseIdsOf("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.preview("user-1", "save10")).hasMessageContaining("Cart is empty");
    }

    // ---- checkout

    @Test
    void applyingLocksTheVoucherChecksTheLearnerAndReportsWhichCoursesTheDiscountCovers() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "30000", null);
        voucher.replaceTargetRules(Set.of(VoucherTargetRule.builder().id("r1").targetType(TargetType.COURSE)
                .matchType(MatchType.ALLOW).targetId("c1").build()));
        when(repository.lockByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(redemptions.countByVoucherIdAndUserId("v1", "user-1")).thenReturn(0L);

        AppliedVoucher applied = service.apply("user-1", "SAVE10", List.of(course("c1", "100000"), course("c2", "50000")));

        assertThat(applied.voucherId()).isEqualTo("v1");
        assertThat(applied.code()).isEqualTo("SAVE10");
        assertThat(applied.discountType()).isEqualTo(DiscountType.FIXED);
        assertThat(applied.eligibleSubtotal()).isEqualByComparingTo("100000");
        assertThat(applied.discountAmount()).isEqualByComparingTo("30000");
        assertThat(applied.discountedCourseIds()).containsExactly("c1");
    }

    @Test
    void applyingAnUnknownCodeIsNotFound() {
        when(repository.lockByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply("user-1", "NOPE", List.of(course("c1", "1000")))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void applyingStopsAtThePerUserLimitBeforeCalculating() {
        Voucher voucher = voucher("v1", "SAVE10", DiscountType.FIXED, "30000", null);
        when(repository.lockByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(redemptions.countByVoucherIdAndUserId("v1", "user-1")).thenReturn(1L);

        assertThatThrownBy(() -> service.apply("user-1", "SAVE10", List.of(course("c1", "100000"))))
                .hasMessageContaining("per-user limit");
    }

    @Test
    void aFirstPurchaseVoucherIsRefusedAfterAPaidOrderOrAClaim() {
        Voucher voucher = voucher("v1", "FIRST", DiscountType.FIXED, "1000", null);
        voucher.setFirstPurchaseOnly(true);
        when(repository.lockByCode("FIRST")).thenReturn(Optional.of(voucher));
        when(redemptions.countByVoucherIdAndUserId("v1", "user-1")).thenReturn(0L);
        when(purchases.hasPaidOrder("user-1")).thenReturn(false);
        when(redemptions.existsFirstPurchaseClaim("user-1")).thenReturn(true);

        assertThatThrownBy(() -> service.apply("user-1", "FIRST", List.of(course("c1", "1000")))).hasMessageContaining("first purchase");
    }

    @Test
    void consumingRecordsTheRedemptionCountsTheUseAndClaimsAFirstPurchase() {
        Voucher voucher = voucher("v1", "FIRST", DiscountType.FIXED, "1000", null);
        voucher.setFirstPurchaseOnly(true);
        when(repository.findById("v1")).thenReturn(Optional.of(voucher));
        AppliedVoucher applied = new AppliedVoucher("v1", "FIRST", DiscountType.FIXED, new BigDecimal("1000"),
                new BigDecimal("5000"), new BigDecimal("1000"), Set.of("c1"), true);

        service.consume(applied, "user-1", "order-1", NOW);

        ArgumentCaptor<VoucherRedemption> redemption = ArgumentCaptor.forClass(VoucherRedemption.class);
        verify(redemptions).save(redemption.capture());
        assertThat(redemption.getValue().getVoucherId()).isEqualTo("v1");
        assertThat(redemption.getValue().getUserId()).isEqualTo("user-1");
        assertThat(redemption.getValue().getOrderId()).isEqualTo("order-1");
        assertThat(redemption.getValue().getEligibleSubtotal()).isEqualByComparingTo("5000");
        assertThat(redemption.getValue().getDiscountAmount()).isEqualByComparingTo("1000");
        assertThat(redemption.getValue().getConsumedAt()).isEqualTo(NOW);
        assertThat(redemption.getValue().isFirstPurchaseClaim()).isTrue();
        assertThat(voucher.getUsedCount()).isEqualTo(1);
        verify(repository).save(voucher);
    }

    private static CreateVoucherCommand create(String code, DiscountType type, String value, String max, long total, long perUser) {
        return new CreateVoucherCommand(code, type, new BigDecimal(value), max == null ? null : new BigDecimal(max), BigDecimal.ZERO, 0,
                total, perUser, false, NOW.minusDays(1), NOW.plusDays(30),
                Set.of(new VoucherTargetRuleCommand(TargetType.COURSE, MatchType.ALLOW, "c1")));
    }

    private static UpdateVoucherCommand update(DiscountType type, String value, String max) {
        return new UpdateVoucherCommand(type, new BigDecimal(value), max == null ? null : new BigDecimal(max), BigDecimal.ZERO, 0,
                100, 1, false, NOW.minusDays(1), NOW.plusDays(30),
                new HashSet<>(Set.of(new VoucherTargetRuleCommand(TargetType.CATEGORY, MatchType.DENY, "cat1"))));
    }

    private static Voucher voucher(String id, String code, DiscountType type, String value, String max) {
        return Voucher.builder().id(id).code(code).discountType(type).discountValue(new BigDecimal(value))
                .maxDiscountAmount(max == null ? null : new BigDecimal(max)).minimumEligibleAmount(BigDecimal.ZERO).minimumEligibleItems(0)
                .totalUsageLimit(100).usedCount(0).perUserLimit(1).validFrom(NOW.minusDays(1)).validTo(NOW.plusDays(30)).active(true).build();
    }

    private static SellableCourseView course(String id, String price) {
        return new SellableCourseView(id, id, new BigDecimal(price), "cat", "i1", true);
    }
}
