package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.CommerceSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface CommerceSubscriptionPersistenceMapper {
    CommerceSubscriptionJpaEntity toEntity(CommerceSubscription domain);

    CommerceSubscription toDomain(CommerceSubscriptionJpaEntity entity);

    // the lock version belongs to the stored row
    @Mapping(target = "optimisticVersion", ignore = true)
    void update(@MappingTarget CommerceSubscriptionJpaEntity entity, CommerceSubscription domain);
}
