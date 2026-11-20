package com.cinx.commerce.payment.application;

import com.cinx.commerce.order.application.PayableOrders;
import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.payment.domain.Payment;
import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.payment.domain.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCallbackService implements IPaymentCallbackService {
    private final PayableOrders orders;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final Clock clock;

    @Override
    @Transactional
    public SettlementOutcome settle(PaymentProviderName provider, ProviderPayment payment) {
        PaymentAttempt attempt = attemptRepository.findByProviderAndProviderReference(provider, payment.providerReference()).orElse(null);
        if (attempt == null) return SettlementOutcome.NOT_FOUND;
        LocalDateTime now = LocalDateTime.now(clock);
        if (!payment.paid()) {
            if (attempt.isCreated()) {
                attempt.markFailed(now);
                attemptRepository.save(attempt);
            }
            return SettlementOutcome.NOT_PAID;
        }
        PayableOrder order = orders.lock(attempt.getOrderId()).orElse(null);
        if (order == null) return SettlementOutcome.NOT_FOUND;
        if (paymentRepository.existsByProviderAndProviderEventId(provider.name(), payment.providerEventId())) {
            return SettlementOutcome.DUPLICATE;
        }
        if (order.totalAmount().compareTo(payment.amount()) != 0 || attempt.getAmount().compareTo(payment.amount()) != 0) {
            return SettlementOutcome.AMOUNT_MISMATCH;
        }
        if (order.cancelled()) return SettlementOutcome.ORDER_CANCELLED;
        if (order.paid()) return SettlementOutcome.DUPLICATE;
        paymentRepository.save(Payment.builder().id(UUID.randomUUID().toString()).orderId(order.id())
                .provider(provider.name()).providerEventId(payment.providerEventId()).amount(payment.amount()).paidAt(now).build());
        attempt.markPaid(now);
        attemptRepository.save(attempt);
        orders.markPaid(order.id(), now);
        return SettlementOutcome.APPLIED;
    }
}
