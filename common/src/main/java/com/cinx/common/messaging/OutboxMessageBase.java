package com.cinx.common.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class OutboxMessageBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Integer eventVersion;

    @Column(nullable = false)
    private String exchangeName;

    @Column(nullable = false)
    private String routingKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String headersJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    private LocalDateTime publishedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
