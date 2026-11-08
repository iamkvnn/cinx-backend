package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface CommerceOrderJpaRepository extends JpaRepository<CommerceOrderJpaEntity, String> {
    @Query(value = "SELECT * FROM commerce_order WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<CommerceOrderJpaEntity> findByIdForUpdate(String id);

    /** Newest first; MySQL puts undated (older pending) orders last on DESC. */
    List<CommerceOrderJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT DISTINCT i.courseId FROM CommerceOrderItemJpaEntity i WHERE i.order.userId = :userId AND i.order.status = com.cinx.commerce.order.domain.OrderStatus.PAID")
    List<String> findPaidCourseIds(String userId);

    Optional<CommerceOrderJpaEntity> findByIdAndUserId(String id, String userId);

    Optional<CommerceOrderJpaEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    boolean existsByUserIdAndStatus(String userId, OrderStatus status);
}
