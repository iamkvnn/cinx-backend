package com.cinx.commerce.payment.application;

import com.cinx.commerce.order.application.PayableOrders;
import com.cinx.commerce.order.application.query.PayableOrder;
import com.cinx.commerce.payment.application.query.PaymentLink;
import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderPaymentService implements IOrderPaymentService {
    private final PayableOrders orders;
    private final PaymentAttemptRepository attemptRepository;
    private final Map<PaymentProviderName, OrderPaymentProvider> providers = new EnumMap<>(PaymentProviderName.class);
    private final Clock clock;

    public OrderPaymentService(PayableOrders orders, PaymentAttemptRepository attemptRepository,
                               List<OrderPaymentProvider> adapters, Clock clock) {
        this.orders = orders;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
        adapters.forEach(adapter -> providers.put(adapter.name(), adapter));
    }

    @Override
    @Transactional
    public PaymentLink createPayment(String userId, String orderId, PaymentProviderName name, String customerIp) {
        // another learner's order answers 404, like the other order endpoints
        PayableOrder order = orders.lockForUser(userId, orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.paid()) throw new ConflictException(ErrorCode.PAYMENT_ALREADY_PAID, "Order is already paid");
        if (!order.pending()) throw new BadRequestException("Only a pending order can be paid");
        OrderPaymentProvider provider = providers.get(name);
        if (provider == null || !provider.available() || !provider.accepts(order.totalAmount())) {
            throw new BadRequestException("This payment method is not available for this order");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<PaymentAttempt> live = attemptRepository.findLiveAttempt(orderId, name, now);
        if (live.isPresent()) return PaymentLink.of(live.get());
        String id = UUID.randomUUID().toString();
        // the gateways accept short alphanumeric ids; this is the attempt id without its dashes
        String reference = id.replace("-", "");
        LocalDateTime expiresAt = now.plus(provider.lifetime());
        OrderPaymentProvider.CreatedPayment created = provider.create(
                new OrderPaymentProvider.PaymentRequest(reference, orderId, order.totalAmount(), expiresAt, customerIp));
        return PaymentLink.of(attemptRepository.save(PaymentAttempt.builder().id(id).orderId(orderId).provider(name)
                .providerReference(reference).status(PaymentAttemptStatus.CREATED).amount(order.totalAmount())
                .paymentUrl(created.paymentUrl()).expiresAt(expiresAt).createdAt(now).updatedAt(now).build()));
    }
}
