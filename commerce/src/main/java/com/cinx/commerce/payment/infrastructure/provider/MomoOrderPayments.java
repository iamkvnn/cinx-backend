package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MoMo e-wallet checkout (captureWallet). MoMo has no documented link expiry, so a link's lifetime is ours.
 * A payment counts only from an IPN whose signature verifies and whose result code is 0.
 */
@Component
public class MomoOrderPayments implements OrderPaymentProvider {
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("50000000");
    private static final String HMAC = "HmacSHA256";

    private final RestClient client;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String frontendBaseUrl;
    private final String publicBaseUrl;

    public MomoOrderPayments(RestClient.Builder builder,
                             @Value("${momo.partner-code:}") String partnerCode,
                             @Value("${momo.access-key:}") String accessKey,
                             @Value("${momo.secret-key:}") String secretKey,
                             @Value("${momo.api-base-url:https://test-payment.momo.vn}") String apiBaseUrl,
                             @Value("${cinx.payment.frontend-base-url:http://localhost:5174}") String frontendBaseUrl,
                             @Value("${cinx.payment.public-base-url:http://localhost:9090}") String publicBaseUrl) {
        this.client = builder.baseUrl(apiBaseUrl).build();
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.frontendBaseUrl = frontendBaseUrl;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public PaymentProviderName name() {
        return PaymentProviderName.MOMO;
    }

    @Override
    public boolean available() {
        return !partnerCode.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
    }

    @Override
    public Duration lifetime() {
        return Duration.ofMinutes(15);
    }

    @Override
    public boolean accepts(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0 && amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
    }

    @Override
    public CreatedPayment create(PaymentRequest request) {
        String amount = request.amount().toBigIntegerExact().toString();
        String orderInfo = "Thanh toan don hang " + request.orderId();
        String redirectUrl = frontendBaseUrl + "/orders/" + request.orderId();
        String ipnUrl = publicBaseUrl + "/api/v1/payments/webhooks/momo";
        // a new orderId and requestId per attempt: MoMo refuses to reuse either (result codes 40 and 41)
        String signed = "accessKey=" + accessKey + "&amount=" + amount + "&extraData=&ipnUrl=" + ipnUrl
                + "&orderId=" + request.reference() + "&orderInfo=" + orderInfo + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl + "&requestId=" + request.reference() + "&requestType=captureWallet";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", request.reference());
        body.put("amount", Long.parseLong(amount));
        body.put("orderId", request.reference());
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("lang", "vi");
        body.put("requestType", "captureWallet");
        body.put("extraData", "");
        body.put("signature", WebhookSigning.hmacHex(HMAC, secretKey, signed));
        JsonNode response = client.post().uri("/v2/gateway/api/create")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
        if (response == null || response.path("resultCode").asInt(-1) != 0 || response.path("payUrl").asText().isBlank()) {
            throw new IllegalStateException("MoMo refused the payment request, resultCode="
                    + (response == null ? "none" : response.path("resultCode").asText()));
        }
        return new CreatedPayment(response.path("payUrl").asText());
    }

    /**
     * Verifies the IPN signature and reads the result. Empty means the notice is not a final outcome (code 9000, authorized only).
     *
     * @throws IllegalArgumentException when the signature or partner code does not match
     */
    public Optional<ProviderPayment> parseIpn(Map<String, Object> ipn) {
        String signed = "accessKey=" + accessKey + "&amount=" + text(ipn, "amount") + "&extraData=" + text(ipn, "extraData")
                + "&message=" + text(ipn, "message") + "&orderId=" + text(ipn, "orderId") + "&orderInfo=" + text(ipn, "orderInfo")
                + "&orderType=" + text(ipn, "orderType") + "&partnerCode=" + text(ipn, "partnerCode") + "&payType=" + text(ipn, "payType")
                + "&requestId=" + text(ipn, "requestId") + "&responseTime=" + text(ipn, "responseTime")
                + "&resultCode=" + text(ipn, "resultCode") + "&transId=" + text(ipn, "transId");
        if (!available() || !WebhookSigning.sameSignature(WebhookSigning.hmacHex(HMAC, secretKey, signed), text(ipn, "signature"))) {
            throw new IllegalArgumentException("Invalid MoMo signature");
        }
        if (!partnerCode.equals(text(ipn, "partnerCode"))) throw new IllegalArgumentException("MoMo partner code mismatch");
        String reference = text(ipn, "orderId");
        int resultCode = parseInt(text(ipn, "resultCode"));
        if (reference.isBlank()) return Optional.empty();
        if (resultCode == 0) {
            return Optional.of(new ProviderPayment(reference, text(ipn, "transId"), decimal(text(ipn, "amount")), true));
        }
        if (resultCode == 9000) return Optional.empty();
        return Optional.of(new ProviderPayment(reference, reference + ":" + resultCode, BigDecimal.ZERO, false));
    }

    private static String text(Map<String, Object> ipn, String key) {
        Object value = ipn.get(key);
        return value == null ? "" : value.toString();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("MoMo result code is not a number", exception);
        }
    }

    private static BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("MoMo amount is not a number", exception);
        }
    }
}
