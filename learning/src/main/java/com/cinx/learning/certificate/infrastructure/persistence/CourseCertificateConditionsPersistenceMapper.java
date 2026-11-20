package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.learning.certificate.domain.CourseCertificateConditions;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CourseCertificateConditionsPersistenceMapper {
    CourseCertificateConditions toDomain(CourseCertificateConditionsJpaEntity entity);

    CourseCertificateConditionsJpaEntity toEntity(CourseCertificateConditions domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget CourseCertificateConditionsJpaEntity entity, CourseCertificateConditions domain);
}
