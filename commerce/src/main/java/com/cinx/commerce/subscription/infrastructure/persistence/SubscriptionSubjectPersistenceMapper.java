package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SubscriptionSubjectPersistenceMapper {
    SubscriptionSubjectJpaEntity toEntity(SubscriptionSubject domain);

    SubscriptionSubject toDomain(SubscriptionSubjectJpaEntity entity);

    void update(@MappingTarget SubscriptionSubjectJpaEntity entity, SubscriptionSubject domain);
}
