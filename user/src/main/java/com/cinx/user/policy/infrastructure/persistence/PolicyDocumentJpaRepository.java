package com.cinx.user.policy.infrastructure.persistence;

import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface PolicyDocumentJpaRepository extends JpaRepository<PolicyDocumentJpaEntity, String> {
    @Query("SELECT COALESCE(MAX(p.versionNumber), 0) FROM PolicyDocumentJpaEntity p WHERE p.slug = :slug")
    Integer findMaxVersionNumberBySlug(String slug);

    Optional<PolicyDocumentJpaEntity> findFirstBySlugAndStatusOrderByVersionNumberDesc(String slug, PolicyStatus status);

    List<PolicyDocumentJpaEntity> findAllByStatusOrderByDisplayOrderAscPublishedAtDesc(PolicyStatus status);

    List<PolicyDocumentJpaEntity> findAllBySlugAndStatus(String slug, PolicyStatus status);

    @Query("""
        SELECT p FROM PolicyDocumentJpaEntity p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:policyType IS NULL OR p.policyType = :policyType)
          AND (:query IS NULL OR p.title LIKE %:query% OR p.slug LIKE %:query% OR p.summary LIKE %:query%)
    """)
    Page<PolicyDocumentJpaEntity> findAllForManagement(PolicyStatus status, PolicyType policyType, String query, Pageable pageable);
}
