package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SubscriptionCommandPersistenceMapper {
    SubscriptionCommandJpaEntity toEntity(SubscriptionCommand domain);

    SubscriptionCommand toDomain(SubscriptionCommandJpaEntity entity);

    void update(@MappingTarget SubscriptionCommandJpaEntity entity, SubscriptionCommand domain);
}
