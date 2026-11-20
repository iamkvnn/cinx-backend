package com.cinx.notification.shared.infrastructure.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inbox_message")
public class InboxMessage {

    @Id
    private String messageId;

    private String status;

    private LocalDateTime processedAt;
}
