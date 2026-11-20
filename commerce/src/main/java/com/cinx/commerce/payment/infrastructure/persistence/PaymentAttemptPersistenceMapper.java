package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.PaymentAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PaymentAttemptPersistenceMapper {
    PaymentAttemptJpaEntity toEntity(PaymentAttempt domain);

    PaymentAttempt toDomain(PaymentAttemptJpaEntity entity);

    void update(@MappingTarget PaymentAttemptJpaEntity entity, PaymentAttempt domain);
}
