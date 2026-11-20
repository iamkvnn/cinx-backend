package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPay redirect payment (API 2.1.0). The link is built and signed locally, so creating it needs no network call.
 * A payment counts only from an IPN whose hash verifies and whose response and transaction codes are both 00.
 */
@Component
public class VnpayOrderPayments implements OrderPaymentProvider {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HMAC = "HmacSHA512";

    private final Clock clock;
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String frontendBaseUrl;

    public VnpayOrderPayments(Clock clock,
                              @Value("${vnpay.tmn-code:}") String tmnCode,
                              @Value("${vnpay.hash-secret:}") String hashSecret,
                              @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                              @Value("${cinx.payment.frontend-base-url:http://localhost:5174}") String frontendBaseUrl) {
        this.clock = clock;
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public PaymentProviderName name() {
        return PaymentProviderName.VNPAY;
    }

    @Override
    public boolean available() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    @Override
    public Duration lifetime() {
        return Duration.ofMinutes(15);
    }

    @Override
    public boolean accepts(BigDecimal amount) {
        return amount.signum() > 0 && amount.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public CreatedPayment create(PaymentRequest request) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", request.amount().movePointRight(2).toBigIntegerExact().toString());
        params.put("vnp_CreateDate", STAMP.format(ZonedDateTime.now(clock).withZoneSameInstant(VIETNAM)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", request.customerIp());
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", "Thanh toan don hang " + request.orderId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", frontendBaseUrl + "/orders/" + request.orderId());
        // required by VNPay; dates are Vietnam time (the doc's Java sample names Etc/GMT+7, which is UTC-7)
        params.put("vnp_ExpireDate", STAMP.format(request.expiresAt().atZone(clock.getZone()).withZoneSameInstant(VIETNAM)));
        params.put("vnp_TxnRef", request.reference());
        String query = signedData(params);
        return new CreatedPayment(payUrl + "?" + query + "&vnp_SecureHash=" + WebhookSigning.hmacHex(HMAC, hashSecret, query));
    }

    /**
     * Verifies the IPN hash and reads the result. The hash covers every {@code vnp_} parameter except the hash itself.
     *
     * @throws IllegalArgumentException when the hash or terminal code does not match
     */
    public Optional<ProviderPayment> parseIpn(Map<String, String> query) {
        Map<String, String> params = new TreeMap<>();
        query.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) params.put(key, value);
        });
        if (!available() || !WebhookSigning.sameSignature(WebhookSigning.hmacHex(HMAC, hashSecret, signedData(params)), query.get("vnp_SecureHash"))) {
            throw new IllegalArgumentException("Invalid VNPay signature");
        }
        if (!tmnCode.equals(params.get("vnp_TmnCode"))) throw new IllegalArgumentException("VNPay terminal mismatch");
        String reference = params.getOrDefault("vnp_TxnRef", "");
        if (reference.isBlank()) return Optional.empty();
        BigDecimal amount;
        try {
            amount = new BigDecimal(params.getOrDefault("vnp_Amount", "")).movePointLeft(2);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("VNPay amount is not a number", exception);
        }
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        boolean paid = "00".equals(responseCode) && "00".equals(params.get("vnp_TransactionStatus"));
        String eventId = paid ? params.getOrDefault("vnp_TransactionNo", "") : reference + ":" + responseCode;
        if (eventId.isBlank()) throw new IllegalArgumentException("VNPay transaction number is missing");
        return Optional.of(new ProviderPayment(reference, eventId, paid ? amount : BigDecimal.ZERO, paid));
    }

    /** Sorted {@code name=value} pairs with encoded values and no empty ones, as in VNPay's own sample; also the query string. */
    private static String signedData(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }
}
