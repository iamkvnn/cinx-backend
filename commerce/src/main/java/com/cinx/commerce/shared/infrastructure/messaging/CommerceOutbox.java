package com.cinx.commerce.shared.infrastructure.messaging;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_outbox", uniqueConstraints = @UniqueConstraint(name = "uk_commerce_event", columnNames = "eventId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommerceOutbox {
    @Id private String eventId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false) private String aggregateId;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
    @Column(nullable = false) @Builder.Default private String exchangeName = "cinx.domain.events";
    private String routingKey;
    @Column(nullable = false) @Builder.Default private int eventVersion = 1;
    @Lob private String headersJson;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) @Builder.Default private String status = "PENDING";
    @Column(nullable = false) @Builder.Default private int attempts = 0;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime publishedAt;
    @Column(length = 1000) private String lastError;
}
