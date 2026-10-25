package com.cinx.auth.identity.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_event_inbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEventInbox {
    @Id @Column(name = "event_id") private String eventId;
    @Column(name = "processed_at", nullable = false) private LocalDateTime processedAt;
}
