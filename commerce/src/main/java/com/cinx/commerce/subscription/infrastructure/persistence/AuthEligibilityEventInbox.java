package com.cinx.commerce.subscription.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_eligibility_event_inbox")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthEligibilityEventInbox {
    @Id @Column(name = "event_id", nullable = false) private String eventId;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
}
