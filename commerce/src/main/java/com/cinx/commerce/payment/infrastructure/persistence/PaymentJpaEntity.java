package com.cinx.commerce.payment.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payment", uniqueConstraints = @UniqueConstraint(name = "uk_payment_provider_event", columnNames = {"provider", "provider_event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentJpaEntity {
    @Id private String id;
    @Column(nullable = false) private String orderId;
    @Column(nullable = false) private String provider;
    @Column(name = "provider_event_id", nullable = false) private String providerEventId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDateTime paidAt;
}
