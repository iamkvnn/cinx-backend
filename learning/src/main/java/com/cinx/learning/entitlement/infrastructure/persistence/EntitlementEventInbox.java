package com.cinx.learning.entitlement.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_entitlement_event_inbox")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementEventInbox {
    @Id private String eventId;
    @Column(nullable = false) private String userId;
    @Column(nullable = false) private long entitlementVersion;
    @Column(nullable = false) private LocalDateTime receivedAt;
}
