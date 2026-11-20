package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The signatures below were computed with {@code openssl dgst -sha256 -hmac SK} over the strings MoMo documents
 * (fields in alphabetical order, accessKey added), not with the adapter's own code.
 */
class MomoOrderPaymentsTest {
    static final String SIGNED_SUCCESS = "71ea87c1b321d9b7581d434a835143b6aa42a1c961c3c3dde0c2c2691d71a9ee";
    static final String SIGNED_FAILURE = "c0505a642f9919a110bef6b2997cb5a6dc6426d38220fb28a45ebe4e0b73633f";
    static final String SIGNED_AUTHORIZED = "b26239292ec177b9d92d2f4c73aceb136466935aa6c050df73a2441a60689b1c";
    static final String SIGNED_CREATE = "0432b5f5cb07d68dad9687bdf46a5760728f653818ff5164320243ca0a341da5";

    MockRestServiceServer server;
    MomoOrderPayments momo;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        momo = new MomoOrderPayments(builder, "PC", "AK", "SK", "https://momo.example.test",
                "https://app.example.test", "https://api.example.test");
    }

    @Test
    void createSendsTheSignedRequestAndReturnsThePayUrl() {
        server.expect(requestTo("https://momo.example.test/v2/gateway/api/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.signature").value(SIGNED_CREATE))
                .andExpect(jsonPath("$.orderId").value("ref1"))
                .andExpect(jsonPath("$.requestId").value("ref1"))
                .andExpect(jsonPath("$.amount").value(199000))
                .andExpect(jsonPath("$.requestType").value("captureWallet"))
                .andExpect(jsonPath("$.redirectUrl").value("https://app.example.test/orders/order-1"))
                .andExpect(jsonPath("$.ipnUrl").value("https://api.example.test/api/v1/payments/webhooks/momo"))
                .andRespond(withSuccess("{\"resultCode\":0,\"payUrl\":\"https://momo.example.test/pay?t=1\"}", MediaType.APPLICATION_JSON));

        var created = momo.create(new OrderPaymentProvider.PaymentRequest("ref1", "order-1", new BigDecimal("199000.00"),
                LocalDateTime.of(2026, 10, 7, 3, 15), "203.0.113.7"));

        assertEquals("https://momo.example.test/pay?t=1", created.paymentUrl());
        server.verify();
    }

    @Test
    void createFailsWhenMomoRefusesTheRequest() {
        server.expect(requestTo("https://momo.example.test/v2/gateway/api/create"))
                .andRespond(withSuccess("{\"resultCode\":1001,\"message\":\"Insufficient\"}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> momo.create(new OrderPaymentProvider.PaymentRequest(
                "ref1", "order-1", new BigDecimal("199000"), LocalDateTime.of(2026, 10, 7, 3, 15), "203.0.113.7")));
    }

    @Test
    void aSignedSuccessNoticeBecomesAPaidPaymentKeyedByMomosTransactionId() {
        ProviderPayment payment = momo.parseIpn(ipn(0, "Successful.", 4088878653L, SIGNED_SUCCESS)).orElseThrow();

        assertEquals("ref1", payment.providerReference());
        assertEquals("4088878653", payment.providerEventId());
        assertEquals(0, new BigDecimal("199000").compareTo(payment.amount()));
        assertTrue(payment.paid());
    }

    @Test
    void aNoticeWhoseAmountWasChangedAfterSigningIsRejected() {
        Map<String, Object> tampered = ipn(0, "Successful.", 4088878653L, SIGNED_SUCCESS);
        tampered.put("amount", 1000);

        assertThrows(IllegalArgumentException.class, () -> momo.parseIpn(tampered));
    }

    @Test
    void aNoticeWithoutOrWithAWrongSignatureIsRejected() {
        Map<String, Object> unsigned = ipn(0, "Successful.", 4088878653L, SIGNED_SUCCESS);
        unsigned.remove("signature");
        Map<String, Object> wrong = ipn(0, "Successful.", 4088878653L, SIGNED_FAILURE);

        assertThrows(IllegalArgumentException.class, () -> momo.parseIpn(unsigned));
        assertThrows(IllegalArgumentException.class, () -> momo.parseIpn(wrong));
    }

    @Test
    void aSignedFailureMarksTheAttemptNotPaid() {
        ProviderPayment payment = momo.parseIpn(ipn(1006, "Denied", 0L, SIGNED_FAILURE)).orElseThrow();

        assertFalse(payment.paid());
        assertEquals("ref1", payment.providerReference());
        assertEquals("ref1:1006", payment.providerEventId());
    }

    @Test
    void anAuthorizedOnlyNoticeIsNotAFinalOutcome() {
        assertTrue(momo.parseIpn(ipn(9000, "Authorized", 4088878653L, SIGNED_AUTHORIZED)).isEmpty());
    }

    @Test
    void withoutKeysTheGatewayIsUnavailableAndVerifiesNothing() {
        MomoOrderPayments unconfigured = new MomoOrderPayments(RestClient.builder(), "", "", "", "https://momo.example.test",
                "https://app.example.test", "https://api.example.test");

        assertFalse(unconfigured.available());
        assertThrows(IllegalArgumentException.class, () -> unconfigured.parseIpn(ipn(0, "Successful.", 4088878653L, SIGNED_SUCCESS)));
    }

    @Test
    void acceptsOnlyWholeVndBetweenMomosFloorAndCeiling() {
        assertFalse(momo.accepts(new BigDecimal("999")));
        assertTrue(momo.accepts(new BigDecimal("1000")));
        assertTrue(momo.accepts(new BigDecimal("50000000.00")));
        assertFalse(momo.accepts(new BigDecimal("50000001")));
        assertFalse(momo.accepts(new BigDecimal("1000.50")));
    }

    /** The notice as Jackson hands it over: numbers stay numbers, the rest are strings. */
    private static Map<String, Object> ipn(int resultCode, String message, long transId, String signature) {
        Map<String, Object> ipn = new HashMap<>();
        ipn.put("partnerCode", "PC");
        ipn.put("orderId", "ref1");
        ipn.put("requestId", "ref1");
        ipn.put("amount", 199000);
        ipn.put("orderInfo", "Thanh toan don hang order-1");
        ipn.put("orderType", "momo_wallet");
        ipn.put("transId", transId);
        ipn.put("resultCode", resultCode);
        ipn.put("message", message);
        ipn.put("payType", "qr");
        ipn.put("responseTime", 1759806000000L);
        ipn.put("extraData", "");
        ipn.put("signature", signature);
        return ipn;
    }
}
