package com.cinx.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.infrastructure.messaging.CommerceOutboxRepository;
import com.cinx.commerce.payment.application.IPaymentCallbackService;
import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import com.cinx.commerce.payment.application.SettlementOutcome;
import com.cinx.commerce.payment.application.ProviderPayment;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CommerceApplicationIT extends AbstractMySqlContainerTest {
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommerceOutboxRepository outbox;
    @Autowired IPaymentCallbackService callbacks;
    @Autowired PaymentAttemptRepository attempts;

    @Test
    void emptyDatabaseMigratesAndJpaSchemaValidates() {
    }

    @Test
    void concurrentRepeatedCallbackCreatesOnePaymentAndOnePaidEvent() throws Exception {
        orders.save(CommerceOrder.builder().id("concurrent-order").userId("user-1")
                .originalAmount(new BigDecimal("100.00")).discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.00")).status(OrderStatus.PENDING).build());
        var now = LocalDateTime.now();
        attempts.save(PaymentAttempt.builder().id("attempt-1").orderId("concurrent-order").provider(PaymentProviderName.STRIPE)
                .providerReference("concurrent-ref").status(PaymentAttemptStatus.CREATED).amount(new BigDecimal("100.00"))
                .paymentUrl("https://pay.example/1").expiresAt(now.plusMinutes(30)).createdAt(now).updatedAt(now).build());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> applyAfterBarrier(ready, start));
            var second = executor.submit(() -> applyAfterBarrier(ready, start));
            ready.await();
            start.countDown();
            var firstResult = first.get();
            var secondResult = second.get();
            assertThat(firstResult == SettlementOutcome.APPLIED ^ secondResult == SettlementOutcome.APPLIED).isTrue();
        }

        assertThat(jdbc.queryForObject("select count(*) from payment", Integer.class)).isEqualTo(1);
        assertThat(outbox.findAll()).singleElement()
                .satisfies(event -> assertThat(event.getEventType()).isEqualTo("CommerceOrderPaid"));
        assertThat(orders.findById("concurrent-order").orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    private SettlementOutcome applyAfterBarrier(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return callbacks.settle(PaymentProviderName.STRIPE,
                new ProviderPayment("concurrent-ref", "provider-event-1", new BigDecimal("100.0"), true));
    }
}
