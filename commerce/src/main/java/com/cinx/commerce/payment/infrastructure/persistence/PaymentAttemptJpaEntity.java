package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payment link issued for an order. A link past {@code expiresAt} is never reused; a new attempt replaces it. */
@Entity
@Table(name = "payment_attempt", uniqueConstraints = @UniqueConstraint(name = "uk_payment_attempt_reference", columnNames = {"provider", "provider_reference"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentAttemptJpaEntity {
    @Id private String id;
    @Column(nullable = false) private String orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentProviderName provider;
    @Column(name = "provider_reference", nullable = false) private String providerReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentAttemptStatus status;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, columnDefinition = "TEXT") private String paymentUrl;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
