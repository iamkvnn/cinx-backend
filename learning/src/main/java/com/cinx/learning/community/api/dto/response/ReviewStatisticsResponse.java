package com.cinx.learning.community.api.dto.response;

import java.util.Map;

public record ReviewStatisticsResponse(
        Long reviewCount,
        Double averageRating,
        Map<String, Long> ratingDistribution,
        Double replyRate
) {
}
