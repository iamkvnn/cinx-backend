package com.cinx.learning.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "CatalogIdempotencyRecord")
@Table(name = "catalog_idempotency_record")
@Getter
@Setter
@NoArgsConstructor
class CatalogIdempotencyRecordJpaEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "idem_key", nullable = false)
    private String idemKey;
    @Column(nullable = false)
    private String operation;
    @Column(name = "request_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String requestHash;
    @Column(name = "resource_id", nullable = false)
    private String resourceId;
    @Column(name = "response_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String responseJson;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
