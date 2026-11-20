package com.cinx.learning.certificate.infrastructure.persistence;

import com.cinx.learning.certificate.domain.CertificateRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CertificateRequestPersistenceMapper {
    CertificateRequest toDomain(CertificateRequestJpaEntity entity);

    CertificateRequestJpaEntity toEntity(CertificateRequest domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget CertificateRequestJpaEntity entity, CertificateRequest domain);
}
