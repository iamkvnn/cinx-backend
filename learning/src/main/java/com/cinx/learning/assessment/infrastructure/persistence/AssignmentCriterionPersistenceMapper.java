package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.AssignmentCriterion;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AssignmentCriterionPersistenceMapper {
    AssignmentCriterion toDomain(AssignmentCriterionJpaEntity entity);

    AssignmentCriterionJpaEntity toEntity(AssignmentCriterion domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget AssignmentCriterionJpaEntity entity, AssignmentCriterion domain);
}
