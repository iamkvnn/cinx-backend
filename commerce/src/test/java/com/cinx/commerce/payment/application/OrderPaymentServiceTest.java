package com.cinx.commerce.payment.application;

import com.cinx.commerce.order.application.PayableOrders;
import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.payment.application.query.PaymentLink;
import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock PayableOrders orders;
    @Mock PaymentAttemptRepository attemptRepository;
    @Mock OrderPaymentProvider provider;
    OrderPaymentService service;

    @BeforeEach
    void setUp() {
        lenient().when(provider.name()).thenReturn(PaymentProviderName.MOMO);
        service = new OrderPaymentService(orders, attemptRepository, List.of(provider),
                Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void issuesALinkForTheStoredTotalAndKeepsTheAttempt() {
        givenOrder(true, false, false, "199000.00");
        givenProviderAcceptsAndCreates();
        when(attemptRepository.save(any(PaymentAttempt.class))).thenAnswer(call -> call.getArgument(0));

        PaymentLink link = service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7");

        ArgumentCaptor<OrderPaymentProvider.PaymentRequest> sent = ArgumentCaptor.forClass(OrderPaymentProvider.PaymentRequest.class);
        verify(provider).create(sent.capture());
        assertEquals(0, new BigDecimal("199000").compareTo(sent.getValue().amount()));
        assertEquals("order-1", sent.getValue().orderId());
        assertEquals("203.0.113.7", sent.getValue().customerIp());
        assertEquals(NOW.plusMinutes(15), sent.getValue().expiresAt());
        // the gateway reference is a dash-free id the gateways accept
        assertFalse(sent.getValue().reference().contains("-"));
        assertEquals(32, sent.getValue().reference().length());

        ArgumentCaptor<PaymentAttempt> saved = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(attemptRepository).save(saved.capture());
        assertEquals(sent.getValue().reference(), saved.getValue().getProviderReference());
        assertEquals(PaymentAttemptStatus.CREATED, saved.getValue().getStatus());
        assertEquals("https://pay.example/1", link.paymentUrl());
        assertEquals(NOW.plusMinutes(15), link.expiresAt());
        assertEquals(PaymentProviderName.MOMO, link.provider());
    }

    @Test
    void aLiveLinkForTheSameOrderAndGatewayIsReusedNotReissued() {
        givenOrder(true, false, false, "199000.00");
        givenProviderAcceptsAndCreates();
        PaymentAttempt live = PaymentAttempt.builder().id("a1").orderId("order-1").provider(PaymentProviderName.MOMO)
                .providerReference("ref").status(PaymentAttemptStatus.CREATED).amount(new BigDecimal("199000.00"))
                .paymentUrl("https://pay.example/live").expiresAt(NOW.plusMinutes(5)).createdAt(NOW).updatedAt(NOW).build();
        when(attemptRepository.findLiveAttempt("order-1", PaymentProviderName.MOMO, NOW)).thenReturn(Optional.of(live));

        PaymentLink link = service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7");

        assertEquals("https://pay.example/live", link.paymentUrl());
        verify(provider, never()).create(any());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void anotherLearnersOrderLooksNonexistent() {
        // the order feature answers nothing for an order that is not the caller's
        when(orders.lockForUser("someone-else", "order-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.createPayment("someone-else", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(provider, never()).create(any());
    }

    @Test
    void aMissingOrderIsNotFound() {
        when(orders.lockForUser("user-1", "order-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
    }

    @Test
    void anAlreadyPaidOrderIsAConflict() {
        givenOrder(false, true, false, "199000.00");

        assertThrows(ConflictException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(provider, never()).create(any());
    }

    @Test
    void aCancelledOrderCannotBePaid() {
        givenOrder(false, false, true, "199000.00");

        assertThrows(BadRequestException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(provider, never()).create(any());
    }

    @Test
    void aGatewayWithoutKeysIsRefused() {
        givenOrder(true, false, false, "199000.00");
        when(provider.available()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(provider, never()).create(any());
    }

    @Test
    void aGatewayThatCannotTakeTheAmountIsRefused() {
        givenOrder(true, false, false, "500.00");
        when(provider.available()).thenReturn(true);
        when(provider.accepts(new BigDecimal("500.00"))).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(provider, never()).create(any());
    }

    @Test
    void aGatewayThatIsNotRegisteredIsRefused() {
        givenOrder(true, false, false, "199000.00");

        assertThrows(BadRequestException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.VNPAY, "203.0.113.7"));
    }

    @Test
    void aGatewayFailurePropagatesAndLeavesNoAttempt() {
        givenOrder(true, false, false, "199000.00");
        when(provider.available()).thenReturn(true);
        when(provider.accepts(any())).thenReturn(true);
        when(provider.lifetime()).thenReturn(Duration.ofMinutes(15));
        when(provider.create(any())).thenThrow(new IllegalStateException("gateway down"));

        assertThrows(IllegalStateException.class,
                () -> service.createPayment("user-1", "order-1", PaymentProviderName.MOMO, "203.0.113.7"));
        verify(attemptRepository, never()).save(any());
    }

    private void givenOrder(boolean pending, boolean paid, boolean cancelled, String total) {
        when(orders.lockForUser("user-1", "order-1"))
                .thenReturn(Optional.of(new PayableOrder("order-1", new BigDecimal(total), pending, paid, cancelled)));
    }

    private void givenProviderAcceptsAndCreates() {
        when(provider.available()).thenReturn(true);
        when(provider.accepts(any())).thenReturn(true);
        lenient().when(provider.lifetime()).thenReturn(Duration.ofMinutes(15));
        lenient().when(provider.create(any())).thenReturn(new OrderPaymentProvider.CreatedPayment("https://pay.example/1"));
    }
}
