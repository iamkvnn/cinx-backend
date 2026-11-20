package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hashes were computed with {@code openssl dgst -sha512 -hmac HS} over the sorted, URL-encoded parameter string
 * VNPay's sample builds, not with the adapter's own code.
 */
class VnpayOrderPaymentsTest {
    static final String PAY_URL = "https://vnpay.example.test/pay";
    static final String CREATE_HASH = "30ba1dad98f1144f16f243a640d2e76183783cab9d39545d858234f0c04fffc872265de171992b43363a13b3106e92ce9853122b4c7d2ccf2948129a93bdaefe";
    static final String SUCCESS_HASH = "250d4c4f6194253036a17e23e31dc8e068af9f335ef8ced1a3aabd24c9a9dc02e995e0ca98373674ebe4ec0bae1daa65cdfe8769e82bc8818e14b591c215330b";
    static final String FAILED_HASH = "707c38a8893807347cdc245daf01386bfd634704daeaaed84ac9c8a09b11c08c767bb6e552e6e63c99419a371e7132ee90f159f31fe41e879aa3ecef3453660c";
    static final String HALF_SUCCESS_HASH = "aac32ceaada5ca9bf645b2e32d3d137533fb466507869fe520646c143b73a3329774a69ce32da6d20b5fdadf71765be4d5950e2358f33de799eed340180249ef";

    VnpayOrderPayments vnpay;

    @BeforeEach
    void setUp() {
        // 03:00 UTC is 10:00 in Vietnam
        vnpay = new VnpayOrderPayments(Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC),
                "TMN1", "HS", PAY_URL, "https://app.example.test");
    }

    @Test
    void createBuildsTheSortedEncodedSignedUrlInVietnamTime() {
        String url = vnpay.create(new OrderPaymentProvider.PaymentRequest("ref1", "order-1", new BigDecimal("199000.00"),
                LocalDateTime.of(2026, 10, 7, 3, 15), "203.0.113.7")).paymentUrl();

        assertEquals(PAY_URL + "?vnp_Amount=19900000&vnp_Command=pay&vnp_CreateDate=20261007100000&vnp_CurrCode=VND"
                + "&vnp_ExpireDate=20261007101500&vnp_IpAddr=203.0.113.7&vnp_Locale=vn"
                + "&vnp_OrderInfo=Thanh+toan+don+hang+order-1&vnp_OrderType=other"
                + "&vnp_ReturnUrl=https%3A%2F%2Fapp.example.test%2Forders%2Forder-1&vnp_TmnCode=TMN1&vnp_TxnRef=ref1"
                + "&vnp_Version=2.1.0&vnp_SecureHash=" + CREATE_HASH, url);
    }

    @Test
    void aSignedSuccessNoticeBecomesAPaidPaymentKeyedByVnpaysTransactionNumber() {
        ProviderPayment payment = vnpay.parseIpn(ipn("00", "00", "14226112", SUCCESS_HASH)).orElseThrow();

        assertEquals("ref1", payment.providerReference());
        assertEquals("14226112", payment.providerEventId());
        // vnp_Amount is multiplied by 100: 19900000 is 199000 VND
        assertEquals(0, new BigDecimal("199000").compareTo(payment.amount()));
        assertTrue(payment.paid());
    }

    @Test
    void aNoticeWhoseAmountWasChangedAfterSigningIsRejected() {
        Map<String, String> tampered = ipn("00", "00", "14226112", SUCCESS_HASH);
        tampered.put("vnp_Amount", "10000");

        assertThrows(IllegalArgumentException.class, () -> vnpay.parseIpn(tampered));
    }

    @Test
    void aNoticeWithoutOrWithAWrongHashIsRejected() {
        Map<String, String> unsigned = ipn("00", "00", "14226112", SUCCESS_HASH);
        unsigned.remove("vnp_SecureHash");

        assertThrows(IllegalArgumentException.class, () -> vnpay.parseIpn(unsigned));
        assertThrows(IllegalArgumentException.class, () -> vnpay.parseIpn(ipn("00", "00", "14226112", FAILED_HASH)));
    }

    @Test
    void aNoticeForAnotherTerminalIsRejected() {
        Map<String, String> other = ipn("00", "00", "14226112", SUCCESS_HASH);
        VnpayOrderPayments otherTerminal = new VnpayOrderPayments(Clock.systemUTC(), "TMN2", "HS", PAY_URL, "https://app.example.test");

        assertThrows(IllegalArgumentException.class, () -> otherTerminal.parseIpn(other));
    }

    @Test
    void aSignedFailureMarksTheAttemptNotPaid() {
        ProviderPayment payment = vnpay.parseIpn(ipn("24", "02", "0", FAILED_HASH)).orElseThrow();

        assertFalse(payment.paid());
        assertEquals("ref1:24", payment.providerEventId());
    }

    @Test
    void bothTheResponseCodeAndTheTransactionStatusMustBeZeroZero() {
        ProviderPayment payment = vnpay.parseIpn(ipn("00", "02", "14226112", HALF_SUCCESS_HASH)).orElseThrow();

        assertFalse(payment.paid());
    }

    @Test
    void withoutKeysTheGatewayIsUnavailableAndVerifiesNothing() {
        VnpayOrderPayments unconfigured = new VnpayOrderPayments(Clock.systemUTC(), "", "", PAY_URL, "https://app.example.test");

        assertFalse(unconfigured.available());
        assertThrows(IllegalArgumentException.class, () -> unconfigured.parseIpn(ipn("00", "00", "14226112", SUCCESS_HASH)));
    }

    @Test
    void acceptsOnlyPositiveWholeVnd() {
        assertTrue(vnpay.accepts(new BigDecimal("199000.00")));
        assertFalse(vnpay.accepts(BigDecimal.ZERO));
        assertFalse(vnpay.accepts(new BigDecimal("10.5")));
    }

    private static Map<String, String> ipn(String responseCode, String transactionStatus, String transactionNo, String hash) {
        Map<String, String> query = new HashMap<>();
        query.put("vnp_Amount", "19900000");
        query.put("vnp_BankCode", "NCB");
        query.put("vnp_OrderInfo", "Thanh toan don hang order-1");
        query.put("vnp_ResponseCode", responseCode);
        query.put("vnp_TmnCode", "TMN1");
        query.put("vnp_TransactionNo", transactionNo);
        query.put("vnp_TransactionStatus", transactionStatus);
        query.put("vnp_TxnRef", "ref1");
        query.put("vnp_SecureHashType", "SHA512");
        query.put("vnp_SecureHash", hash);
        return query;
    }
}
