package com.cinx.commerce.shared.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, String> {
}
