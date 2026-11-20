package com.cinx.commerce.order.application;

import com.cinx.commerce.cart.application.CartContents;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.command.CheckoutCommand;
import com.cinx.commerce.order.application.query.OrderView;
import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.application.DomainEvents;
import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.application.AppliedVoucher;
import com.cinx.commerce.voucher.application.VoucherCheckout;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);
    // SHA-256 of the text the service fingerprints, from `printf NO_VOUCHER | shasum -a 256` and the same for SAVE10
    static final String NO_VOUCHER_FINGERPRINT = "ca1cf30a3102753e1ab09c2fda4c7241b99c85a614bd84a9e5eff1202687dcd1";
    static final String SAVE10_FINGERPRINT = "6f862afc1e2aa88c5bfc0e772b58a38840e49f081a7a7b26dab3b6efbcc72de7";

    @Mock OrderRepository orders;
    @Mock CartContents cart;
    @Mock SellableCourses courses;
    @Mock VoucherCheckout vouchers;
    @Mock DomainEvents events;
    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, cart, courses, vouchers, new OrderPaidPublisher(events),
                Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "platformFeePercent", new BigDecimal("30"));
    }

    @Test
    void checkoutPricesTheCartSnapshotsEachLineAndClearsTheCart() {
        givenCart("c1", "c2");
        givenCourse("c1", "100000.00", "i1");
        givenCourse("c2", "50000.00", "i2");

        OrderView view = service.checkout("user-1", "key-1", null);

        ArgumentCaptor<CommerceOrder> saved = ArgumentCaptor.forClass(CommerceOrder.class);
        verify(orders).save(saved.capture());
        CommerceOrder order = saved.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getOriginalAmount()).isEqualByComparingTo("150000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("150000");
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getItems()).extracting("courseId", "titleSnapshot", "instructorId", "platformFeePercent")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("c1", "Title c1", "i1", new BigDecimal("30")),
                        org.assertj.core.groups.Tuple.tuple("c2", "Title c2", "i2", new BigDecimal("30")));
        assertThat(order.getItems()).extracting("allocatedAmount").usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("100000.00"), new BigDecimal("50000.00"));
        assertThat(view.totalAmount()).isEqualByComparingTo("150000");
        verify(cart).clear("user-1");
        verifyNoInteractions(vouchers, events);
    }

    @Test
    void theSameKeyWithTheSameRequestReturnsTheStoredOrderAndChangesNothing() {
        CommerceOrder stored = CommerceOrder.builder().id("o1").userId("user-1").requestFingerprint(NO_VOUCHER_FINGERPRINT)
                .originalAmount(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
                .status(OrderStatus.PENDING).items(List.of()).build();
        when(orders.findByUserIdAndIdempotencyKey("user-1", "key-1")).thenReturn(Optional.of(stored));

        assertThat(service.checkout("user-1", "key-1", new CheckoutCommand("  ")).id()).isEqualTo("o1");

        verify(orders, never()).save(any());
        verifyNoInteractions(cart, vouchers, events);
    }

    @Test
    void theSameKeyWithAnotherVoucherIsRefused() {
        CommerceOrder stored = CommerceOrder.builder().id("o1").userId("user-1").requestFingerprint(NO_VOUCHER_FINGERPRINT)
                .items(List.of()).build();
        when(orders.findByUserIdAndIdempotencyKey("user-1", "key-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.checkout("user-1", "key-1", new CheckoutCommand("save10")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("another request");
    }

    @Test
    void theVoucherCodeIsTrimmedAndUpperCasedBeforeItIsFingerprinted() {
        CommerceOrder stored = CommerceOrder.builder().id("o1").userId("user-1").requestFingerprint(SAVE10_FINGERPRINT)
                .originalAmount(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
                .status(OrderStatus.PENDING).items(List.of()).build();
        when(orders.findByUserIdAndIdempotencyKey("user-1", "key-1")).thenReturn(Optional.of(stored));

        assertThat(service.checkout("user-1", "key-1", new CheckoutCommand("  save10 ")).id()).isEqualTo("o1");
    }

    @Test
    void anIdempotencyKeyIsRequiredAndBounded() {
        for (String key : new String[]{null, "", "  ", "k".repeat(256)}) {
            assertThatThrownBy(() -> service.checkout("user-1", key, null)).isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(orders, cart);
    }

    @Test
    void anEmptyCartIsRefused() {
        when(cart.courseIdsOf("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkout("user-1", "key-1", null)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void aCourseTheLearnerAlreadyOwnsBlocksTheCheckout() {
        givenCart("c1", "c2");
        when(orders.findPaidCourseIds("user-1")).thenReturn(List.of("c2"));

        assertThatThrownBy(() -> service.checkout("user-1", "key-1", null)).isInstanceOf(ConflictException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void aCourseThatStoppedSellingBlocksTheCheckout() {
        givenCart("c1");
        when(courses.findSellable("c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkout("user-1", "key-1", null)).isInstanceOf(BadRequestException.class);
        verify(orders, never()).save(any());
        verify(cart, never()).clear(any());
    }

    @Test
    void aVoucherDiscountIsSnapshottedSplitAcrossTheCoveredLinesAndConsumedAfterTheOrderIsStored() {
        givenCart("c1", "c2");
        givenCourse("c1", "100000.00", "i1");
        givenCourse("c2", "50000.00", "i2");
        AppliedVoucher applied = new AppliedVoucher("v1", "SAVE10", DiscountType.FIXED, new BigDecimal("30000.00"),
                new BigDecimal("100000.00"), new BigDecimal("30000.00"), Set.of("c1"), false);
        when(vouchers.apply(eq("user-1"), eq("SAVE10"), any())).thenReturn(applied);

        OrderView view = service.checkout("user-1", "key-1", new CheckoutCommand(" save10 "));

        ArgumentCaptor<CommerceOrder> saved = ArgumentCaptor.forClass(CommerceOrder.class);
        var order = inOrder(orders, vouchers, cart);
        order.verify(orders).save(saved.capture());
        order.verify(vouchers).consume(applied, "user-1", saved.getValue().getId(), NOW);
        order.verify(cart).clear("user-1");
        assertThat(saved.getValue().getDiscountAmount()).isEqualByComparingTo("30000");
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("120000");
        // the whole discount lands on the one covered line: 100000 - 30000
        assertThat(saved.getValue().getItems().get(0).getAllocatedAmount()).isEqualByComparingTo("70000");
        assertThat(saved.getValue().getItems().get(1).getAllocatedAmount()).isEqualByComparingTo("50000");
        assertThat(view.voucher()).isEqualTo(new OrderView.VoucherSnapshot("SAVE10", DiscountType.FIXED, new BigDecimal("30000.00")));
        verifyNoInteractions(events);
    }

    @Test
    void aVoucherThatCoversTheWholeCartPaysTheOrderAtOnceWithItsPaidEvent() {
        givenCart("c1");
        givenCourse("c1", "100000.00", "i1");
        AppliedVoucher applied = new AppliedVoucher("v1", "FREE", DiscountType.FIXED, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("100000.00"), Set.of("c1"), true);
        when(vouchers.apply(eq("user-1"), eq("FREE"), any())).thenReturn(applied);

        OrderView view = service.checkout("user-1", "key-1", new CheckoutCommand("FREE"));

        assertThat(view.status()).isEqualTo(OrderStatus.PAID);
        assertThat(view.paidAt()).isEqualTo(NOW);
        assertThat(view.totalAmount()).isEqualByComparingTo("0");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(events).publish(eq("CommerceOrderPaid"), eq(view.id()), payload.capture());
        assertThat(payload.getValue()).containsEntry("userId", "user-1").containsEntry("orderId", view.id());
    }

    @Test
    void anUnknownVoucherFailsTheCheckoutBeforeAnythingIsStored() {
        givenCart("c1");
        givenCourse("c1", "100000.00", "i1");
        when(vouchers.apply(eq("user-1"), eq("NOPE"), any())).thenThrow(new NotFoundException("Voucher not found"));

        assertThatThrownBy(() -> service.checkout("user-1", "key-1", new CheckoutCommand("nope"))).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
        verify(cart, never()).clear(any());
    }

    @Test
    void cancellingAPendingOrderRecordsWhenAndLeavesTheVoucherConsumed() {
        CommerceOrder pending = CommerceOrder.builder().id("o1").userId("user-1").status(OrderStatus.PENDING).items(List.of()).build();
        when(orders.lockById("o1")).thenReturn(Optional.of(pending));
        when(orders.save(pending)).thenReturn(pending);

        OrderView view = service.cancel("user-1", "o1");

        assertThat(view.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(view.cancelledAt()).isEqualTo(NOW);
        verifyNoInteractions(vouchers);
    }

    @Test
    void anotherLearnersOrderCannotBeCancelledAndLooksNonexistent() {
        CommerceOrder pending = CommerceOrder.builder().id("o1").userId("user-1").status(OrderStatus.PENDING).items(List.of()).build();
        when(orders.lockById("o1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.cancel("someone-else", "o1")).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void onlyAPendingOrderCanBeCancelled() {
        for (OrderStatus status : new OrderStatus[]{OrderStatus.PAID, OrderStatus.CANCELLED}) {
            CommerceOrder order = CommerceOrder.builder().id("o-" + status).userId("user-1").status(status).items(List.of()).build();
            when(orders.lockById("o-" + status)).thenReturn(Optional.of(order));
            assertThatThrownBy(() -> service.cancel("user-1", "o-" + status)).as(status.name()).isInstanceOf(BadRequestException.class);
        }
        verify(orders, never()).save(any());
    }

    @Test
    void readingAnotherLearnersOrderIsNotFoundButAnAdministratorMayReadAny() {
        CommerceOrder order = CommerceOrder.builder().id("o1").userId("user-1").status(OrderStatus.PAID).items(List.of()).build();
        when(orders.findByIdAndUserId("o1", "someone-else")).thenReturn(Optional.empty());
        when(orders.findById("o1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.findById("someone-else", "o1")).isInstanceOf(NotFoundException.class);
        assertThat(service.findByIdAny("o1").id()).isEqualTo("o1");
    }

    private void givenCart(String... courseIds) {
        when(cart.courseIdsOf("user-1")).thenReturn(List.of(courseIds));
    }

    private void givenCourse(String id, String price, String instructor) {
        when(courses.findSellable(id)).thenReturn(Optional.of(new SellableCourseView(id, "Title " + id, new BigDecimal(price), "cat", instructor, true)));
    }
}
