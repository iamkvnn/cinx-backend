package com.cinx.learning.shared.infrastructure.messaging;

import com.cinx.common.messaging.OutboxMessageBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "outbox_message", indexes = {
        @Index(name = "idx_learning_outbox_status_next", columnList = "status,nextAttemptAt")
})
public class OutboxMessage extends OutboxMessageBase {
}
