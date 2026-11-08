package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The signatures were computed with {@code openssl dgst -sha256 -hmac whsec_test} over {@code <timestamp>.<body>}, the
 * scheme Stripe documents, with the timestamp 1791342000 (the fixed clock), not with the verifier's own code.
 */
class StripeOrderPaymentsTest {
    static final Instant NOW = Instant.parse("2026-10-07T03:00:00Z");
    static final String COMPLETED = "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\"cs_test_1\"}}}";
    static final String COMPLETED_SIG = "t=1791342000,v1=7e650574de0069e9c4058fb2cd8635ef43521d1cf0ca642124b92e0204dd4df1";
    static final String EXPIRED = "{\"id\":\"evt_2\",\"type\":\"checkout.session.expired\",\"data\":{\"object\":{\"id\":\"cs_test_2\",\"client_reference_id\":\"ref1\"}}}";
    static final String EXPIRED_SIG = "t=1791342000,v1=731337d9a51facb8fe98460c562755370cec0a343fcc526b6aa16f39369be59b";
    static final String UNRELATED = "{\"id\":\"evt_3\",\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_1\"}}}";
    static final String UNRELATED_SIG = "t=1791342000,v1=cb71bc423e45f6b261fe1ab93a289004b6117180de786be722269955b8af6425";

    MockRestServiceServer server;
    StripeOrderPayments stripe;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        stripe = new StripeOrderPayments(builder, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC), "sk_test", "whsec_test",
                300, "2024-06-20", "https://stripe.example.test", "https://app.example.test");
    }

    @Test
    void createSendsAZeroDecimalVndSessionWithOurReferenceAndReturnUrls() {
        server.expect(requestTo("https://stripe.example.test/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk_test"))
                .andExpect(header("Idempotency-Key", "cinx_order_ref1"))
                .andExpect(content().formDataContains(Map.of(
                        "mode", "payment",
                        "line_items[0][price_data][currency]", "vnd",
                        // VND has no minor unit, so 199000 VND is sent as 199000
                        "line_items[0][price_data][unit_amount]", "199000",
                        "line_items[0][quantity]", "1",
                        "client_reference_id", "ref1",
                        "metadata[order_id]", "order-1",
                        "success_url", "https://app.example.test/orders/order-1",
                        "cancel_url", "https://app.example.test/payment/cancel?orderId=order-1",
                        // 03:31 UTC is 1791342000 + 31 * 60
                        "expires_at", "1791343860")))
                .andRespond(withSuccess("{\"id\":\"cs_test_1\",\"url\":\"https://checkout.stripe.example.test/pay/cs_test_1\"}", MediaType.APPLICATION_JSON));

        var created = stripe.create(new OrderPaymentProvider.PaymentRequest("ref1", "order-1", new BigDecimal("199000.00"),
                LocalDateTime.of(2026, 10, 7, 3, 31), "203.0.113.7"));

        assertEquals("https://checkout.stripe.example.test/pay/cs_test_1", created.paymentUrl());
        server.verify();
    }

    @Test
    void aPaidSessionIsFetchedAgainAndBecomesAPaidPayment() {
        expectSession("{\"payment_status\":\"paid\",\"amount_total\":199000,\"currency\":\"vnd\",\"client_reference_id\":\"ref1\"}");

        ProviderPayment payment = stripe.parseWebhook(bytes(COMPLETED), COMPLETED_SIG).orElseThrow();

        assertEquals("ref1", payment.providerReference());
        assertEquals("evt_1", payment.providerEventId());
        assertEquals(0, new BigDecimal("199000").compareTo(payment.amount()));
        assertTrue(payment.paid());
        server.verify();
    }

    @Test
    void theAmountComesFromTheFetchedSessionNotFromTheWebhookBody() {
        // the signed body carries no amount at all, so nothing in it could overstate what was paid
        expectSession("{\"payment_status\":\"paid\",\"amount_total\":5000,\"currency\":\"vnd\",\"client_reference_id\":\"ref1\"}");

        ProviderPayment payment = stripe.parseWebhook(bytes(COMPLETED), COMPLETED_SIG).orElseThrow();

        assertEquals(0, new BigDecimal("5000").compareTo(payment.amount()));
    }

    @Test
    void aDelayedMethodsCompletedButUnpaidSessionWaitsForTheLaterEvent() {
        expectSession("{\"payment_status\":\"unpaid\",\"amount_total\":199000,\"currency\":\"vnd\",\"client_reference_id\":\"ref1\"}");

        assertTrue(stripe.parseWebhook(bytes(COMPLETED), COMPLETED_SIG).isEmpty());
    }

    @Test
    void aSessionInAnotherCurrencyIsRejected() {
        expectSession("{\"payment_status\":\"paid\",\"amount_total\":199000,\"currency\":\"usd\",\"client_reference_id\":\"ref1\"}");

        assertThrows(IllegalArgumentException.class, () -> stripe.parseWebhook(bytes(COMPLETED), COMPLETED_SIG));
    }

    @Test
    void anExpiredSessionMarksTheAttemptNotPaid() {
        ProviderPayment payment = stripe.parseWebhook(bytes(EXPIRED), EXPIRED_SIG).orElseThrow();

        assertFalse(payment.paid());
        assertEquals("ref1", payment.providerReference());
        assertEquals("evt_2", payment.providerEventId());
    }

    @Test
    void anEventAboutSomethingElseIsIgnored() {
        assertTrue(stripe.parseWebhook(bytes(UNRELATED), UNRELATED_SIG).isEmpty());
    }

    @Test
    void aBodyChangedAfterSigningIsRejectedBeforeAnythingIsFetched() {
        String tampered = COMPLETED.replace("cs_test_1", "cs_test_9");

        assertThrows(IllegalArgumentException.class, () -> stripe.parseWebhook(bytes(tampered), COMPLETED_SIG));
        server.verify();
    }

    @Test
    void aMissingMalformedOrStaleSignatureIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> stripe.parseWebhook(bytes(COMPLETED), null));
        assertThrows(IllegalArgumentException.class, () -> stripe.parseWebhook(bytes(COMPLETED), "v1=abc"));
        // one hour before the fixed clock, beyond the 300 second tolerance
        assertThrows(IllegalArgumentException.class, () -> stripe.parseWebhook(bytes(COMPLETED), "t=1791338400,v1=" + COMPLETED_SIG.substring(COMPLETED_SIG.indexOf("v1=") + 3)));
    }

    @Test
    void withoutItsSecretsTheGatewayIsUnavailableAndVerifiesNothing() {
        StripeOrderPayments unconfigured = new StripeOrderPayments(RestClient.builder(), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC),
                "sk_test", "", 300, "2024-06-20", "https://stripe.example.test", "https://app.example.test");

        assertFalse(unconfigured.available());
        assertThrows(IllegalArgumentException.class, () -> unconfigured.parseWebhook(bytes(COMPLETED), COMPLETED_SIG));
    }

    @Test
    void acceptsOnlyPositiveWholeVnd() {
        assertTrue(stripe.accepts(new BigDecimal("199000.00")));
        assertFalse(stripe.accepts(BigDecimal.ZERO));
        assertFalse(stripe.accepts(new BigDecimal("199000.50")));
    }

    private void expectSession(String body) {
        server.expect(requestTo("https://stripe.example.test/v1/checkout/sessions/cs_test_1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk_test"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static byte[] bytes(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
