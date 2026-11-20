package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PaymentPersistenceMapper {
    PaymentJpaEntity toEntity(Payment domain);

    Payment toDomain(PaymentJpaEntity entity);

    void update(@MappingTarget PaymentJpaEntity entity, Payment domain);
}
