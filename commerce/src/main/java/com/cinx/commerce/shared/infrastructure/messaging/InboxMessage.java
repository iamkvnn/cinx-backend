package com.cinx.commerce.shared.infrastructure.messaging;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_inbox")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InboxMessage {
    @Id private String eventId;
    @Column(nullable = false) private LocalDateTime processedAt;
}
