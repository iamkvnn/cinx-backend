package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface CurrentSubscriptionPointerPersistenceMapper {
    CurrentSubscriptionPointerJpaEntity toEntity(CurrentSubscriptionPointer domain);

    CurrentSubscriptionPointer toDomain(CurrentSubscriptionPointerJpaEntity entity);

    // the lock version belongs to the stored row
    @Mapping(target = "optimisticVersion", ignore = true)
    void update(@MappingTarget CurrentSubscriptionPointerJpaEntity entity, CurrentSubscriptionPointer domain);
}
