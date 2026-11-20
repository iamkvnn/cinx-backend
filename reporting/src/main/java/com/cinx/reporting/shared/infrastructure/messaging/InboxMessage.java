package com.cinx.reporting.shared.infrastructure.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_message")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InboxMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
}
