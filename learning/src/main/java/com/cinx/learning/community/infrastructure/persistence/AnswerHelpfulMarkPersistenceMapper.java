package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.AnswerHelpfulMark;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AnswerHelpfulMarkPersistenceMapper {
    AnswerHelpfulMark toDomain(AnswerHelpfulMarkJpaEntity entity);

    AnswerHelpfulMarkJpaEntity toEntity(AnswerHelpfulMark domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget AnswerHelpfulMarkJpaEntity entity, AnswerHelpfulMark domain);
}
