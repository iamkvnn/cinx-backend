package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.RoleGrant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface IdentityAccountPersistenceMapper {
    IdentityAccountJpaEntity toEntity(IdentityAccount account);

    /** The account without its grants; the adapter adds the ones it loaded. */
    @Mapping(target = "grants", ignore = true)
    IdentityAccount toDomain(IdentityAccountJpaEntity entity);

    /** Copies what the domain may change onto the stored row; the key and the lock version stay the row's own. */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget IdentityAccountJpaEntity entity, IdentityAccount account);

    RoleGrant toGrant(IdentityAccountRoleJpaEntity entity);

    IdentityAccountRoleJpaEntity toEntity(RoleGrant grant, String userId);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roleCode", ignore = true)
    void update(@MappingTarget IdentityAccountRoleJpaEntity entity, RoleGrant grant);
}
