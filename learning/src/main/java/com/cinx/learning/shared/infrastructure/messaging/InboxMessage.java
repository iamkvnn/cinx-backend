package com.cinx.learning.shared.infrastructure.messaging;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "learning_inbox")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InboxMessage {
    @Id private String eventId;
    @Column(nullable = false) private LocalDateTime processedAt;
}
