package com.cinx.learning.community.api.dto.response;

import java.util.List;

public record CourseQnAStatisticsResponse(
        Long questionsInRange,
        Long answersInRange,
        Long unansweredQuestionCount,
        Double instructorAnswerRate,
        List<StatisticsByTimeResponse> questionsByTime
) {
}
