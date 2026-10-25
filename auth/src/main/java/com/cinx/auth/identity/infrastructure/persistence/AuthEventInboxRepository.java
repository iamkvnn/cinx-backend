package com.cinx.auth.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthEventInboxRepository extends JpaRepository<AuthEventInbox, String> {
}
