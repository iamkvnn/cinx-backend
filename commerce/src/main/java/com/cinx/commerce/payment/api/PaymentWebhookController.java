package com.cinx.commerce.payment.api;

import com.cinx.commerce.payment.application.IPaymentCallbackService;
import com.cinx.commerce.payment.application.PaymentNoticeReader;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.payment.application.SettlementOutcome;

import com.cinx.commerce.payment.application.ProviderPayment;
import com.cinx.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Provider-to-server payment notices for course orders. Each route is public because the gateway's own signature is its
 * authentication: nothing here marks an order paid before the adapter has verified it. A browser redirect is never proof.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {
    private final PaymentNoticeReader notices;
    private final IPaymentCallbackService settlement;

    @PostMapping(value = "/stripe", consumes = "application/json")
    ResponseEntity<Void> stripe(@RequestHeader(value = "Stripe-Signature", required = false) String signature,
                                @RequestBody byte[] rawBody) {
        Optional<ProviderPayment> payment;
        try {
            payment = notices.readStripe(rawBody, signature);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid payment webhook");
        }
        // a validly signed notice is acknowledged even when it is rejected, or Stripe would retry it for days
        payment.ifPresent(found -> settle(PaymentProviderName.STRIPE, found));
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/momo", consumes = "application/json")
    ResponseEntity<Void> momo(@RequestBody Map<String, Object> ipn) {
        Optional<ProviderPayment> payment;
        try {
            payment = notices.readMomo(ipn);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid payment webhook");
        }
        payment.ifPresent(found -> settle(PaymentProviderName.MOMO, found));
        // MoMo documents 204 as the only acknowledgement
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vnpay")
    Map<String, String> vnpay(@RequestParam Map<String, String> query) {
        Optional<ProviderPayment> payment;
        try {
            payment = notices.readVnpay(query);
        } catch (IllegalArgumentException exception) {
            return reply("97", "Invalid signature");
        }
        if (payment.isEmpty()) return reply("01", "Order not found");
        return switch (settle(PaymentProviderName.VNPAY, payment.get())) {
            case APPLIED, NOT_PAID -> reply("00", "Confirm Success");
            case DUPLICATE -> reply("02", "Order already confirmed");
            case NOT_FOUND -> reply("01", "Order not found");
            case AMOUNT_MISMATCH -> reply("04", "Invalid amount");
            case ORDER_CANCELLED -> reply("99", "Order cannot be paid");
        };
    }

    private SettlementOutcome settle(PaymentProviderName provider, ProviderPayment payment) {
        SettlementOutcome outcome = settlement.settle(provider, payment);
        if (outcome == SettlementOutcome.AMOUNT_MISMATCH || outcome == SettlementOutcome.ORDER_CANCELLED
                || outcome == SettlementOutcome.NOT_FOUND) {
            log.warn("Payment notice not applied: provider={} reference={} outcome={}", provider, payment.providerReference(), outcome);
        }
        return outcome;
    }

    private static Map<String, String> reply(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
