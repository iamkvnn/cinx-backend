package com.cinx.notification.notification.infrastructure.messaging.consumer;

import com.cinx.common.dto.ApiResponse;
import com.cinx.notification.notification.infrastructure.client.UserClient;
import com.cinx.notification.notification.infrastructure.client.dto.user.UserDto;
import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.notification.infrastructure.messaging.event.payment.PaymentEvent;
import com.cinx.notification.dispatch.application.NotificationFormatter;
import com.cinx.notification.dispatch.application.NotificationMessage;
import com.cinx.notification.dispatch.application.INotificationDispatchService;
import com.cinx.notification.shared.infrastructure.messaging.IdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationListener {

    private final UserClient userClient;
    private final INotificationDispatchService dispatchService;
    private final IdempotencyService idempotencyService;
    private final NotificationFormatter notificationFormatter;

    @RabbitListener(queues = "notification.payment.queue", ackMode = "MANUAL")
    public void handlePaymentSuccess(PaymentEvent event, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                     @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        log.info("Received payment success event for order: {}", event.getOrderId());
        try {
            if (idempotencyService.isProcessed(messageId)) {
                channel.basicAck(tag, false);
                return;
            }
            String userId = event.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new IllegalStateException("Payment event is missing userId");
            }

            ApiResponse<UserDto> userResponse = userClient.getUserById(userId);
            if (userResponse == null || !userResponse.success() || userResponse.data() == null) {
                throw new IllegalStateException("Failed to fetch user details for userId: " + userId);
            }

            UserDto user = userResponse.data();
            String formattedPrice = event.getAmount() != null
                    ? formatMoney(event.getAmount())
                    : "0 VND";
            NotificationMessage notification = notificationFormatter.paymentSucceeded(
                    event.getOrderId(), formattedPrice, user.name());

            NotificationContext ctx = NotificationContext.builder()
                    .channels(List.of("EMAIL", "IN_APP"))
                    .emailPayload(notification.emailPayload(user.email()))
                    .inAppPayload(notification.inAppPayload(List.of(userId)))
                    .build();

            dispatchService.dispatch(ctx);
            idempotencyService.markSuccess(messageId);
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Error processing payment success event: ", e);
            try {
                channel.basicNack(tag, false, false);
            } catch (Exception ex) {
                log.error("Error nacking message", ex);
            }
        }
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(amount) + " VND";
    }
}
