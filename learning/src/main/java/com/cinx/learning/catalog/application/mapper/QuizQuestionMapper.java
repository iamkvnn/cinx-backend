package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateQuizQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizQuestionCommand;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import com.cinx.learning.catalog.domain.QuizQuestion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuizQuestionMapper extends
        BaseMapper<QuizQuestion, QuizQuestionView>,
        CreateMapper<QuizQuestion, CreateQuizQuestionCommand>,
        UpdateMapper<QuizQuestion, UpdateQuizQuestionCommand> {
}

