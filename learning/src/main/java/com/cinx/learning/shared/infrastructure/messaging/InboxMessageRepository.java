package com.cinx.learning.shared.infrastructure.messaging;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, String> {
}
