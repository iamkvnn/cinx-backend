package com.cinx.commerce.payment.api;

import com.cinx.commerce.payment.application.IPaymentCallbackService;
import com.cinx.commerce.payment.application.PaymentNoticeReader;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.payment.application.SettlementOutcome;

import com.cinx.commerce.payment.application.ProviderPayment;
import com.cinx.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {
    static final ProviderPayment PAID = new ProviderPayment("ref1", "evt-1", new BigDecimal("199000"), true);

    @Mock PaymentNoticeReader notices;
    @Mock IPaymentCallbackService settlement;
    PaymentWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentWebhookController(notices, settlement);
    }

    @Test
    void stripeWithABadSignatureIsRefusedAndSettlesNothing() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        when(notices.readStripe(body, "bad")).thenThrow(new IllegalArgumentException("Invalid webhook signature"));

        assertThrows(BadRequestException.class, () -> controller.stripe("bad", body));
        verifyNoInteractions(settlement);
    }

    @Test
    void stripeSettlesAVerifiedPaymentAndAcknowledges() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        when(notices.readStripe(body, "sig")).thenReturn(Optional.of(PAID));
        when(settlement.settle(PaymentProviderName.STRIPE, PAID)).thenReturn(SettlementOutcome.APPLIED);

        ResponseEntity<Void> response = controller.stripe("sig", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(settlement).settle(PaymentProviderName.STRIPE, PAID);
    }

    @Test
    void stripeAcknowledgesAVerifiedNoticeEvenWhenItIsRejectedSoItIsNotRetriedForDays() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        when(notices.readStripe(body, "sig")).thenReturn(Optional.of(PAID));
        when(settlement.settle(PaymentProviderName.STRIPE, PAID)).thenReturn(SettlementOutcome.AMOUNT_MISMATCH);

        assertEquals(HttpStatus.OK, controller.stripe("sig", body).getStatusCode());
    }

    @Test
    void stripeIgnoresAnEventThatIsNotAPaymentOutcome() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        when(notices.readStripe(body, "sig")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.OK, controller.stripe("sig", body).getStatusCode());
        verifyNoInteractions(settlement);
    }

    @Test
    void momoWithABadSignatureIsRefusedAndSettlesNothing() {
        Map<String, Object> ipn = Map.of("orderId", "ref1");
        when(notices.readMomo(ipn)).thenThrow(new IllegalArgumentException("Invalid MoMo signature"));

        assertThrows(BadRequestException.class, () -> controller.momo(ipn));
        verifyNoInteractions(settlement);
    }

    @Test
    void momoAnswersNoContentAsItsDocumentationRequires() {
        Map<String, Object> ipn = Map.of("orderId", "ref1");
        when(notices.readMomo(ipn)).thenReturn(Optional.of(PAID));
        when(settlement.settle(PaymentProviderName.MOMO, PAID)).thenReturn(SettlementOutcome.APPLIED);

        assertEquals(HttpStatus.NO_CONTENT, controller.momo(ipn).getStatusCode());
        verify(settlement).settle(PaymentProviderName.MOMO, PAID);
    }

    @Test
    void vnpayRepliesWithTheDocumentedCodeForEachOutcome() {
        Map<String, String> query = Map.of("vnp_TxnRef", "ref1");
        when(notices.readVnpay(query)).thenReturn(Optional.of(PAID));

        expectReply(query, SettlementOutcome.APPLIED, "00");
        expectReply(query, SettlementOutcome.NOT_PAID, "00");
        expectReply(query, SettlementOutcome.DUPLICATE, "02");
        expectReply(query, SettlementOutcome.NOT_FOUND, "01");
        expectReply(query, SettlementOutcome.AMOUNT_MISMATCH, "04");
        expectReply(query, SettlementOutcome.ORDER_CANCELLED, "99");
    }

    @Test
    void vnpayRepliesInvalidSignatureWithoutSettling() {
        Map<String, String> query = Map.of("vnp_TxnRef", "ref1");
        when(notices.readVnpay(query)).thenThrow(new IllegalArgumentException("Invalid VNPay signature"));

        assertEquals("97", controller.vnpay(query).get("RspCode"));
        verify(settlement, never()).settle(any(), any());
    }

    private void expectReply(Map<String, String> query, SettlementOutcome outcome, String code) {
        when(settlement.settle(PaymentProviderName.VNPAY, PAID)).thenReturn(outcome);
        assertEquals(code, controller.vnpay(query).get("RspCode"), "outcome " + outcome);
    }
}
