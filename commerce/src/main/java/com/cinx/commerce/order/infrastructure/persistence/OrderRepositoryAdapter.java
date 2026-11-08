package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrderRepositoryAdapter implements OrderRepository {
    private final CommerceOrderJpaRepository jpa;
    private final OrderPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceOrder> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceOrder> findByIdAndUserId(String id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceOrder> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey) {
        return jpa.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceOrder> findAllByUserId(String userId) {
        return jpa.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<CommerceOrder> lockById(String id) {
        return jpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPaidCourseIds(String userId) {
        return jpa.findPaidCourseIds(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndStatus(String userId, OrderStatus status) {
        return jpa.existsByUserIdAndStatus(userId, status);
    }

    /** Inserts a new order with its lines, or writes the order's changes onto the stored row and leaves its lines alone. */
    @Override
    @Transactional
    public CommerceOrder save(CommerceOrder order) {
        CommerceOrderJpaEntity stored = jpa.findById(order.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(order)));
        }
        mapper.update(stored, order);
        return mapper.toDomain(jpa.save(stored));
    }
}
