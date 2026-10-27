package com.cinx.learning.community.application.query;

import java.util.Map;

public record ReviewStatisticsView(
        Long reviewCount,
        Double averageRating,
        Map<String, Long> ratingDistribution,
        Double replyRate
) {
}
