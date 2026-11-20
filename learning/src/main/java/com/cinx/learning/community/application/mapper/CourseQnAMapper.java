package com.cinx.learning.community.application.mapper;

import com.cinx.learning.community.application.command.CreateQuestionCommand;
import com.cinx.learning.community.application.command.UpdateQuestionCommand;
import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.command.UpdateAnswerCommand;
import com.cinx.learning.community.application.query.QuestionView;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.CourseAnswer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseQnAMapper {
    QuestionView toDto(CourseQuestion entity);
    CourseQuestion toModel(CreateQuestionCommand request);
    void partialUpdate(@MappingTarget CourseQuestion entity, UpdateQuestionCommand dto);

    AnswerView toDto(CourseAnswer entity);
    CourseAnswer toModel(CreateAnswerCommand request);
    void partialUpdate(@MappingTarget CourseAnswer entity, UpdateAnswerCommand dto);
}
