package com.cinx.reporting.reporting.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountProjectionRepository extends JpaRepository<AccountProjection, String> {
    long countByStatus(String status);
}
