package com.cinx.learning.featured.application.command;

import java.util.List;

public record UpsertFeaturedPathCommand(
        String name,

        String roleTitle,

        Integer estimatedMonths,

        Integer hoursPerWeek,

        Boolean active,

        List< String> courseIds,

        String description,

        List<  String> outcomes
) {
}
