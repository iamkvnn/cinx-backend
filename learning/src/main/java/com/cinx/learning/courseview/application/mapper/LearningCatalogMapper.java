package com.cinx.learning.courseview.application.mapper;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LearningCatalogMapper {
    com.cinx.learning.courseview.application.query.CourseView toLearning(
            com.cinx.learning.catalog.application.query.CourseView source);

    com.cinx.learning.courseview.application.query.LessonView toLearning(
            com.cinx.learning.catalog.application.query.LessonView source);

    com.cinx.learning.courseview.application.query.QuizLessonView toLearning(
            com.cinx.learning.catalog.application.query.QuizLessonView source);

    com.cinx.learning.courseview.application.query.VideoLessonView toLearning(
            com.cinx.learning.catalog.application.query.VideoLessonView source);
}
