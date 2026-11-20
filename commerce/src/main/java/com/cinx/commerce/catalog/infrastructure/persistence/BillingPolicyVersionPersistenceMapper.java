package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.BillingPolicyVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface BillingPolicyVersionPersistenceMapper {
    BillingPolicyVersionJpaEntity toEntity(BillingPolicyVersion domain);

    BillingPolicyVersion toDomain(BillingPolicyVersionJpaEntity entity);

    void update(@MappingTarget BillingPolicyVersionJpaEntity entity, BillingPolicyVersion domain);
}
