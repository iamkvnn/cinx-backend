package com.cinx.notification.notification.infrastructure.messaging.event.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String userId;
    @JsonAlias("totalAmount")
    private BigDecimal amount;
    private PaymentStatus status;
}
