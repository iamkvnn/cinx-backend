package com.cinx.learning.catalog.application.mapper;

import com.cinx.learning.catalog.application.command.CreateLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateLessonCommand;
import com.cinx.learning.catalog.application.query.LessonView;
import com.cinx.learning.catalog.domain.Lesson;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface LessonMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stableId", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "prerequisiteIds", ignore = true)
    Lesson toModel(CreateLessonCommand request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stableId", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    @Mapping(target = "lessonType", ignore = true)
    @Mapping(target = "prerequisiteIds", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    void partialUpdate(@MappingTarget Lesson lesson, UpdateLessonCommand request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "sectionId", ignore = true)
    Lesson cloneLesson(Lesson lesson);

    @Mapping(target = "id", source = "stableId")
    @Mapping(target = "videoDurationSeconds", ignore = true)
    LessonView toResponse(Lesson lesson);
}

