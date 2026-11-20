package com.cinx.commerce.payment.application;

import com.cinx.commerce.order.application.PayableOrders;
import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.payment.domain.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {
    static final Instant NOW = Instant.parse("2026-10-07T03:00:00Z");
    static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 10, 7, 3, 0);
    static final PaymentProviderName STRIPE = PaymentProviderName.STRIPE;

    @Mock PayableOrders orders;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentAttemptRepository attemptRepository;
    PaymentCallbackService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCallbackService(orders, paymentRepository, attemptRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void paidNoticeMarksOrderAndAttemptPaidAndRecordsThePayment() {
        PaymentAttempt attempt = attempt("100.00");
        givenAttemptAndOrder(attempt, order(true, false, false, "100.00"));

        // 100.0 and 100.00 are the same money; the comparison must not look at the scale
        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.0"), true));

        assertEquals(SettlementOutcome.APPLIED, outcome);
        assertEquals(PaymentAttemptStatus.PAID, attempt.getStatus());
        assertEquals(NOW_LOCAL, attempt.getUpdatedAt());
        verify(orders).markPaid("order-1", NOW_LOCAL);
        verify(paymentRepository).save(argThat(saved -> saved.getProvider().equals("STRIPE")
                && saved.getProviderEventId().equals("evt-1") && saved.getOrderId().equals("order-1")
                && saved.getPaidAt().equals(NOW_LOCAL)));
    }

    @Test
    void repeatedProviderEventIsANoOp() {
        givenAttemptAndOrder(attempt("100.00"), order(true, false, false, "100.00"));
        when(paymentRepository.existsByProviderAndProviderEventId("STRIPE", "evt-1")).thenReturn(true);

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.DUPLICATE, outcome);
        verify(orders, never()).markPaid(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void anOrderAlreadyPaidByAnotherEventGrantsNothingTwice() {
        givenAttemptAndOrder(attempt("100.00"), order(false, true, false, "100.00"));

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-2", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.DUPLICATE, outcome);
        verify(orders, never()).markPaid(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void aOneUnitDifferenceInTheOrderTotalIsRejected() {
        givenAttemptAndOrder(attempt("100.00"), order(true, false, false, "100.00"));

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.01"), true));

        assertEquals(SettlementOutcome.AMOUNT_MISMATCH, outcome);
        verify(orders, never()).markPaid(any(), any());
    }

    @Test
    void anAmountThatMatchesTheOrderButNotTheIssuedLinkIsRejected() {
        // the order total changed after the link was issued; the old link must not settle it
        givenAttemptAndOrder(attempt("90.00"), order(true, false, false, "100.00"));

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.AMOUNT_MISMATCH, outcome);
        verify(orders, never()).markPaid(any(), any());
    }

    @Test
    void moneyArrivingAfterCancellationGrantsNothing() {
        givenAttemptAndOrder(attempt("100.00"), order(false, false, true, "100.00"));

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.ORDER_CANCELLED, outcome);
        verify(orders, never()).markPaid(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void anUnknownReferenceTouchesNoOrder() {
        when(attemptRepository.findByProviderAndProviderReference(STRIPE, "ref-x")).thenReturn(Optional.empty());

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-x", "evt-1", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.NOT_FOUND, outcome);
        verifyNoInteractions(orders, paymentRepository);
    }

    @Test
    void anAttemptWhoseOrderIsGoneIsNotFound() {
        when(attemptRepository.findByProviderAndProviderReference(STRIPE, "ref-1")).thenReturn(Optional.of(attempt("100.00")));
        when(orders.lock("order-1")).thenReturn(Optional.empty());

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-1", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.NOT_FOUND, outcome);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void aReferenceIssuedForAnotherGatewayIsNotFound() {
        // the lookup is by (provider, reference): a MoMo notice cannot settle a Stripe attempt
        when(attemptRepository.findByProviderAndProviderReference(PaymentProviderName.MOMO, "ref-1")).thenReturn(Optional.empty());

        SettlementOutcome outcome = service.settle(PaymentProviderName.MOMO, new ProviderPayment("ref-1", "9", new BigDecimal("100.00"), true));

        assertEquals(SettlementOutcome.NOT_FOUND, outcome);
        verifyNoInteractions(orders);
    }

    @Test
    void aFailureMarksTheLinkFailedAndLeavesTheOrderPending() {
        PaymentAttempt attempt = attempt("100.00");
        when(attemptRepository.findByProviderAndProviderReference(STRIPE, "ref-1")).thenReturn(Optional.of(attempt));

        SettlementOutcome outcome = service.settle(STRIPE, new ProviderPayment("ref-1", "evt-f", BigDecimal.ZERO, false));

        assertEquals(SettlementOutcome.NOT_PAID, outcome);
        assertEquals(PaymentAttemptStatus.FAILED, attempt.getStatus());
        verify(attemptRepository).save(attempt);
        verifyNoInteractions(orders, paymentRepository);
    }

    @Test
    void aLateFailureNeverUndoesAPaidAttempt() {
        PaymentAttempt attempt = attempt("100.00");
        attempt.setStatus(PaymentAttemptStatus.PAID);
        when(attemptRepository.findByProviderAndProviderReference(STRIPE, "ref-1")).thenReturn(Optional.of(attempt));

        service.settle(STRIPE, new ProviderPayment("ref-1", "evt-f", BigDecimal.ZERO, false));

        assertEquals(PaymentAttemptStatus.PAID, attempt.getStatus());
        verify(attemptRepository, never()).save(any());
    }

    private void givenAttemptAndOrder(PaymentAttempt attempt, PayableOrder order) {
        when(attemptRepository.findByProviderAndProviderReference(STRIPE, "ref-1")).thenReturn(Optional.of(attempt));
        when(orders.lock("order-1")).thenReturn(Optional.of(order));
    }

    private static PayableOrder order(boolean pending, boolean paid, boolean cancelled, String total) {
        return new PayableOrder("order-1", new BigDecimal(total), pending, paid, cancelled);
    }

    private static PaymentAttempt attempt(String amount) {
        LocalDateTime created = LocalDateTime.of(2026, 10, 7, 2, 50);
        return PaymentAttempt.builder().id("attempt-1").orderId("order-1").provider(STRIPE).providerReference("ref-1")
                .status(PaymentAttemptStatus.CREATED).amount(new BigDecimal(amount)).paymentUrl("https://pay.example/1")
                .expiresAt(created.plusMinutes(31)).createdAt(created).updatedAt(created).build();
    }
}
