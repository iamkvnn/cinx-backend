package com.cinx.learning.assessment.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.assessment.application.query.QuizSessionView;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuizSessionMapper extends BaseMapper<QuizSession, QuizSessionView> {
    @Override
    @Mapping(target = "quizSessionSubmission", ignore = true)
    QuizSessionView toDto(QuizSession model);

    /** The submission lives in its own repository, so the caller hands in what it loaded. */
    @Mapping(target = ".", source = "session")
    @Mapping(target = "id", source = "session.id")
    @Mapping(target = "userId", source = "session.userId")
    @Mapping(target = "quizSessionSubmission", source = "submission")
    QuizSessionView toDto(QuizSession session, QuizSessionSubmission submission);
}
