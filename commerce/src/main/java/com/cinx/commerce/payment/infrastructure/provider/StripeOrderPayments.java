package com.cinx.commerce.payment.infrastructure.provider;

import com.cinx.commerce.payment.application.OrderPaymentProvider;
import com.cinx.commerce.payment.application.ProviderPayment;

import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.shared.infrastructure.provider.StripeSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Stripe hosted Checkout for one-time course orders. VND is a zero-decimal currency, so the order total is sent as is.
 * A payment counts only from a signed webhook whose session is fetched again by id.
 */
@Component
public class StripeOrderPayments implements OrderPaymentProvider {
    /** Stripe rejects a session that lives less than 30 minutes, so the margin covers clock drift and latency. */
    private static final Duration LIFETIME = Duration.ofMinutes(31);

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String secretKey;
    private final String apiVersion;
    private final String frontendBaseUrl;
    private final StripeSignatureVerifier signatureVerifier;

    public StripeOrderPayments(RestClient.Builder builder,
                               ObjectMapper objectMapper,
                               Clock clock,
                               @Value("${stripe.secret-key:}") String secretKey,
                               @Value("${stripe.order-webhook-secret:}") String webhookSecret,
                               @Value("${stripe.webhook-tolerance-seconds:300}") long toleranceSeconds,
                               @Value("${stripe.api-version:2024-06-20}") String apiVersion,
                               @Value("${stripe.api-base-url:https://api.stripe.com}") String apiBaseUrl,
                               @Value("${cinx.payment.frontend-base-url:http://localhost:5174}") String frontendBaseUrl) {
        this.client = builder.baseUrl(apiBaseUrl).build();
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.secretKey = secretKey;
        this.apiVersion = apiVersion;
        this.frontendBaseUrl = frontendBaseUrl;
        this.signatureVerifier = webhookSecret == null || webhookSecret.isBlank()
                ? null : new StripeSignatureVerifier(webhookSecret, clock, toleranceSeconds);
    }

    @Override
    public PaymentProviderName name() {
        return PaymentProviderName.STRIPE;
    }

    @Override
    public boolean available() {
        return secretKey != null && !secretKey.isBlank() && signatureVerifier != null;
    }

    @Override
    public Duration lifetime() {
        return LIFETIME;
    }

    @Override
    public boolean accepts(BigDecimal amount) {
        return amount.signum() > 0 && amount.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public CreatedPayment create(PaymentRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("line_items[0][price_data][currency]", "vnd");
        form.add("line_items[0][price_data][product_data][name]", "Cinx course order");
        form.add("line_items[0][price_data][unit_amount]", request.amount().setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", frontendBaseUrl + "/orders/" + request.orderId());
        form.add("cancel_url", frontendBaseUrl + "/payment/cancel?orderId=" + request.orderId());
        form.add("client_reference_id", request.reference());
        form.add("metadata[order_id]", request.orderId());
        form.add("expires_at", String.valueOf(request.expiresAt().atZone(clock.getZone()).toEpochSecond()));
        JsonNode body = client.post().uri("/v1/checkout/sessions")
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion);
                    headers.set("Idempotency-Key", "cinx_order_" + request.reference()); })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(JsonNode.class);
        if (body == null || body.path("url").asText().isBlank()) {
            throw new IllegalStateException("Stripe returned an invalid Checkout Session");
        }
        return new CreatedPayment(body.path("url").asText());
    }

    /**
     * Verifies the signature over the raw body, then reads the event. Empty means the event is not about a payment outcome.
     *
     * @throws IllegalArgumentException when the signature is missing, wrong, stale or the body is not an event
     */
    public Optional<ProviderPayment> parseWebhook(byte[] rawBody, String signature) {
        if (signatureVerifier == null) throw new IllegalArgumentException("Stripe order webhook is not configured");
        signatureVerifier.verify(rawBody, signature);
        JsonNode event;
        try {
            event = objectMapper.readTree(rawBody);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Webhook body is not JSON", exception);
        }
        String eventId = event.path("id").asText();
        JsonNode session = event.path("data").path("object");
        switch (event.path("type").asText()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                JsonNode current = fetchSession(session.path("id").asText());
                // a delayed method completes the session as "unpaid" and sends async_payment_succeeded later
                if ("unpaid".equals(current.path("payment_status").asText())) return Optional.empty();
                String reference = current.path("client_reference_id").asText();
                if (reference.isBlank() || eventId.isBlank()) return Optional.empty();
                if (!"vnd".equalsIgnoreCase(current.path("currency").asText())) {
                    throw new IllegalArgumentException("Unexpected payment currency");
                }
                return Optional.of(new ProviderPayment(reference, eventId, new BigDecimal(current.path("amount_total").asText("0")), true));
            }
            case "checkout.session.async_payment_failed", "checkout.session.expired" -> {
                String reference = session.path("client_reference_id").asText();
                if (reference.isBlank() || eventId.isBlank()) return Optional.empty();
                return Optional.of(new ProviderPayment(reference, eventId, BigDecimal.ZERO, false));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private JsonNode fetchSession(String sessionId) {
        if (sessionId.isBlank()) throw new IllegalArgumentException("Webhook session has no id");
        JsonNode body = client.get().uri("/v1/checkout/sessions/{id}", sessionId)
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion); })
                .retrieve().body(JsonNode.class);
        if (body == null) throw new IllegalStateException("Stripe returned no Checkout Session");
        return body;
    }
}
