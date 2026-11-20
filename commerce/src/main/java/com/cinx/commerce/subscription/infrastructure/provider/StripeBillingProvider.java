package com.cinx.commerce.subscription.infrastructure.provider;

import com.cinx.commerce.shared.infrastructure.provider.StripeSignatureVerifier;
import com.cinx.commerce.subscription.application.BillingProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/** Stripe hosted Checkout adapter. Domain services depend only on BillingProvider. */
@Component
@ConditionalOnProperty(name = "cinx.subscription.provider", havingValue = "STRIPE")
@ConditionalOnProperty(name = "cinx.subscription.billing-enabled", havingValue = "true")
public class StripeBillingProvider implements BillingProvider {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final StripeSignatureVerifier signatureVerifier;
    private final String secretKey;
    private final String apiVersion;
    private final Clock clock;

    public StripeBillingProvider(RestClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 Clock clock,
                                 @Value("${stripe.secret-key:}") String secretKey,
                                 @Value("${stripe.webhook-secret:}") String webhookSecret,
                                 @Value("${stripe.webhook-tolerance-seconds:300}") long toleranceSeconds,
                                 @Value("${stripe.api-version:2024-06-20}") String apiVersion,
                                 @Value("${stripe.api-base-url:https://api.stripe.com}") String apiBaseUrl) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe provider requires stripe.secret-key");
        }
        this.secretKey = secretKey;
        this.client = builder.baseUrl(apiBaseUrl).build();
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.apiVersion = apiVersion;
        this.signatureVerifier = new StripeSignatureVerifier(webhookSecret, clock, toleranceSeconds);
    }

    @Override
    public CheckoutSession createSubscriptionCheckout(CheckoutCommand command) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("line_items[0][price]", command.providerPriceReference());
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", command.successPath());
        form.add("cancel_url", command.cancelPath());
        form.add("client_reference_id", command.commandId());
        JsonNode body = client.post().uri("/v1/checkout/sessions")
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion);
                    headers.set("Idempotency-Key", "cinx_" + command.commandId()); })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(JsonNode.class);
        if (body == null || body.path("id").asText().isBlank() || body.path("url").asText().isBlank()) {
            throw new IllegalStateException("Stripe returned an invalid Checkout Session");
        }
        return new CheckoutSession(body.path("id").asText(), body.path("url").asText(),
                Instant.ofEpochSecond(body.path("expires_at").asLong(Instant.now(clock).plusSeconds(900).getEpochSecond())));
    }

    @Override
    public ChangeQuote previewChange(ChangeCommand command) {
        // Stripe computes the final proration at mutation time.  Keep the quote
        // provider-neutral and short lived; the command still requires the
        // provider confirmation before access changes.
        return new ChangeQuote(BigDecimal.ZERO, "VND", Instant.now(clock).plusSeconds(300), command.commandId());
    }

    @Override
    public ProviderSubscription changeSubscription(ChangeCommand command) {
        JsonNode body = client.post().uri("/v1/subscriptions/{id}", command.providerSubscriptionRef())
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion);
                    headers.set("Idempotency-Key", "cinx_" + command.commandId()); })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form("items[0][price]", command.providerPriceReference(),
                        "proration_behavior", "create_prorations"))
                .retrieve().body(JsonNode.class);
        return toProviderSubscription(body);
    }

    @Override
    public ProviderSubscription cancelAtPeriodEnd(String providerSubscriptionRef) {
        return cancelAtPeriodEnd(providerSubscriptionRef, "cancel_" + providerSubscriptionRef);
    }

    @Override
    public ProviderSubscription cancelAtPeriodEnd(String providerSubscriptionRef, String commandId) {
        JsonNode body = client.post().uri("/v1/subscriptions/{id}", providerSubscriptionRef)
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion);
                    headers.set("Idempotency-Key", "cinx_" + commandId); })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form("cancel_at_period_end", "true"))
                .retrieve().body(JsonNode.class);
        return toProviderSubscription(body);
    }

    @Override
    public ProviderSubscription resume(String providerSubscriptionRef) {
        return resume(providerSubscriptionRef, "resume_" + providerSubscriptionRef);
    }

    @Override
    public ProviderSubscription resume(String providerSubscriptionRef, String commandId) {
        JsonNode body = client.post().uri("/v1/subscriptions/{id}", providerSubscriptionRef)
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion);
                    headers.set("Idempotency-Key", "cinx_" + commandId); })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form("cancel_at_period_end", "false"))
                .retrieve().body(JsonNode.class);
        return toProviderSubscription(body);
    }

    @Override
    public ProviderSubscription fetchSubscription(String providerSubscriptionRef) {
        JsonNode body = client.get().uri("/v1/subscriptions/{id}", providerSubscriptionRef)
                .headers(headers -> { headers.setBearerAuth(secretKey); headers.set("Stripe-Version", apiVersion); })
                .retrieve().body(JsonNode.class);
        if (body == null) throw new IllegalStateException("Stripe returned an empty subscription");
        return toProviderSubscription(body);
    }

    private ProviderSubscription toProviderSubscription(JsonNode body) {
        if (body == null || body.path("id").asText().isBlank()) {
            throw new IllegalStateException("Stripe returned an invalid subscription");
        }
        long periodStart = body.path("current_period_start").asLong(0L);
        long periodEnd = body.path("current_period_end").asLong(0L);
        long providerVersion = body.path("updated").asLong(body.path("created").asLong(0L));
        return new ProviderSubscription(body.path("id").asText(), body.path("status").asText(),
                periodStart > 0 ? Instant.ofEpochSecond(periodStart) : null,
                periodEnd > 0 ? Instant.ofEpochSecond(periodEnd) : null,
                body.path("cancel_at_period_end").asBoolean(false), providerVersion);
    }

    private MultiValueMap<String, String> form(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Stripe form values must be key/value pairs");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < values.length; index += 2) {
            form.add(values[index], values[index + 1]);
        }
        return form;
    }

    @Override
    public NormalizedWebhook verifyAndParseWebhook(byte[] rawBody, ProviderHeaders headers) {
        signatureVerifier.verify(rawBody, headers.signature());
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode object = root.path("data").path("object");
            long providerObjectVersion = object.path("updated").asLong(root.path("created").asLong());
            String objectReference = object.path("subscription").asText();
            if (objectReference.isBlank()) {
                objectReference = object.path("id").asText();
            }
            return new NormalizedWebhook(root.path("id").asText(), objectReference,
                    providerObjectVersion, root.path("type").asText(),
                    object.path("status").asText(), Instant.ofEpochSecond(root.path("created").asLong()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Stripe webhook payload", exception);
        }
    }
}
