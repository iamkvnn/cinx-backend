package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateQuizLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizLessonCommand;
import com.cinx.learning.catalog.application.query.QuizLessonView;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import com.cinx.learning.catalog.domain.QuizLesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuizMapper extends BaseMapper<QuizLesson, QuizLessonView>,
        CreateMapper<QuizLesson, CreateQuizLessonCommand>,
        UpdateMapper<QuizLesson, UpdateQuizLessonCommand> {

    @Override
    @Mapping(target = "lessonId", source = "lessonId")
    @Mapping(target = "questions", ignore = true)
    QuizLessonView toDto(QuizLesson quizLesson);

    /** The questions live in their own repository, so the caller hands them in. */
    @Mapping(target = "lessonId", source = "quizLesson.lessonId")
    @Mapping(target = "questions", source = "questions")
    QuizLessonView toDto(QuizLesson quizLesson, List<QuizQuestionView> questions);
}

