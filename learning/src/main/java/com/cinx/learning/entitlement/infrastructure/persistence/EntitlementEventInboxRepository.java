package com.cinx.learning.entitlement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementEventInboxRepository extends JpaRepository<EntitlementEventInbox, String> {
}
