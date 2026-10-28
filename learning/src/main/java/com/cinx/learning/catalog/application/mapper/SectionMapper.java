package com.cinx.learning.catalog.application.mapper;

import com.cinx.learning.catalog.application.command.CreateSectionCommand;
import com.cinx.learning.catalog.application.command.UpdateSectionCommand;
import com.cinx.learning.catalog.application.query.SectionView;
import com.cinx.learning.catalog.domain.Section;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SectionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stableId", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "draftId", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    Section toModel(CreateSectionCommand request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stableId", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "draftId", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    void partialUpdate(@MappingTarget Section section, UpdateSectionCommand request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "draftId", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    Section cloneSection(Section section);

    @Mapping(target = "id", source = "stableId")
    SectionView toResponse(Section section);
}

