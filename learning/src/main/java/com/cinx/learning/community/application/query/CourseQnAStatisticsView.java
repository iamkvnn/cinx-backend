package com.cinx.learning.community.application.query;

import java.util.List;

public record CourseQnAStatisticsView(
        Long questionsInRange,
        Long answersInRange,
        Long unansweredQuestionCount,
        Double instructorAnswerRate,
        List<StatisticsByTimeView> questionsByTime
) {
}
