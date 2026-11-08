package com.cinx.commerce.subscription.api;

import com.cinx.commerce.subscription.application.BillingWebhookService;
import com.cinx.commerce.subscription.application.BillingProvider;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/billing/webhooks")
public class BillingWebhookController {
    private final ObjectProvider<BillingProvider> provider;
    private final BillingWebhookService webhookService;

    @PostMapping(value = "/{providerName}", consumes = "application/json")
    public ResponseEntity<Void> receive(
            @PathVariable String providerName,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestHeader(value = "X-Provider-Account", defaultValue = "default") String account,
            @RequestBody byte[] rawBody) {
        if (!"STRIPE".equalsIgnoreCase(providerName)) {
            throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Billing provider is not enabled");
        }
        BillingProvider billingProvider = provider.getIfAvailable();
        if (billingProvider == null) {
            throw new BadRequestException(ErrorCode.PLAN_OR_PRICE_UNAVAILABLE, "Billing provider is not enabled");
        }
        try {
            BillingProvider.NormalizedWebhook event = billingProvider.verifyAndParseWebhook(
                    rawBody, new BillingProvider.ProviderHeaders(signature, null, account));
            webhookService.accept("STRIPE", account, rawBody, event);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Invalid billing webhook");
        }
    }
}
