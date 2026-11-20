package com.cinx.commerce.order.application;

import com.cinx.commerce.order.domain.CommerceOrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLineSharesTest {
    static final BigDecimal FEE = new BigDecimal("30");

    @Test
    void theDiscountIsSplitByListPriceAcrossTheCoveredLinesOnly() {
        List<CommerceOrderItem> items = List.of(item("c1", "100.00"), item("c2", "200.00"), item("c3", "300.00"));

        OrderLineShares.allocate(items, Set.of("c1", "c2"), new BigDecimal("90.00"), FEE);

        // covered subtotal 300: c1 takes 90 * 100 / 300 = 30, the last covered line (c2) takes the remaining 60
        assertThat(allocated(items)).containsExactly(new BigDecimal("70.00"), new BigDecimal("140.00"), new BigDecimal("300.00"));
    }

    @Test
    void theLastCoveredLineTakesTheRoundingRemainderSoTheSharesAddUpExactly() {
        List<CommerceOrderItem> items = List.of(item("c1", "10.00"), item("c2", "10.00"), item("c3", "10.00"));

        OrderLineShares.allocate(items, Set.of("c1", "c2", "c3"), new BigDecimal("10.00"), FEE);

        // 10 * 10 / 30 = 3.33 (HALF_UP) for the first two lines, the last takes 10 - 3.33 - 3.33 = 3.34
        assertThat(allocated(items)).containsExactly(new BigDecimal("6.67"), new BigDecimal("6.67"), new BigDecimal("6.66"));
        BigDecimal paidAfterVoucher = allocated(items).stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(paidAfterVoucher).isEqualByComparingTo("20.00");
    }

    @Test
    void aShareNeverTakesMoreThanTheLineCosts() {
        List<CommerceOrderItem> items = List.of(item("c1", "50.00"));

        OrderLineShares.allocate(items, Set.of("c1"), new BigDecimal("80.00"), FEE);

        assertThat(allocated(items)).containsExactly(new BigDecimal("0.00"));
    }

    @Test
    void noDiscountLeavesEveryLineAtItsPriceButStillSnapshotsTheFee() {
        List<CommerceOrderItem> items = List.of(item("c1", "100.00"), item("c2", "200.00"));

        OrderLineShares.allocate(items, Set.of(), BigDecimal.ZERO, FEE);

        assertThat(allocated(items)).containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"));
        assertThat(items).allSatisfy(line -> assertThat(line.getPlatformFeePercent()).isEqualByComparingTo("30"));
    }

    @Test
    void aDiscountWithNoCoveredLineChangesNothing() {
        List<CommerceOrderItem> items = List.of(item("c1", "100.00"));

        OrderLineShares.allocate(items, Set.of("other"), new BigDecimal("40.00"), FEE);

        assertThat(allocated(items)).containsExactly(new BigDecimal("100.00"));
    }

    @Test
    void aNullDiscountIsNoDiscount() {
        List<CommerceOrderItem> items = List.of(item("c1", "100.00"));

        OrderLineShares.allocate(items, Set.of("c1"), null, FEE);

        assertThat(allocated(items)).containsExactly(new BigDecimal("100.00"));
    }

    private static List<BigDecimal> allocated(List<CommerceOrderItem> items) {
        return items.stream().map(CommerceOrderItem::getAllocatedAmount).toList();
    }

    private static CommerceOrderItem item(String courseId, String price) {
        return CommerceOrderItem.builder().id("i-" + courseId).courseId(courseId).priceSnapshot(new BigDecimal(price)).build();
    }
}
