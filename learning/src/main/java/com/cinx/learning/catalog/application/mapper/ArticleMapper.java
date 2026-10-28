package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateArticleLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateArticleLessonCommand;
import com.cinx.learning.catalog.application.query.ArticleLessonView;
import com.cinx.learning.catalog.domain.ArticleLesson;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ArticleMapper extends
        BaseMapper<ArticleLesson, ArticleLessonView>,
        CreateMapper<ArticleLesson, CreateArticleLessonCommand>,
        UpdateMapper<ArticleLesson, UpdateArticleLessonCommand> {
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "articleUrl", ignore = true)
    @Override
    ArticleLesson toModel(CreateArticleLessonCommand createDto);

    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "articleUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Override
    void partialUpdate(@MappingTarget ArticleLesson model, UpdateArticleLessonCommand updateDto);
}

