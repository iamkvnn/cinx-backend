package com.cinx.learning.catalog.application.query;

/**
 * Category as listed by the public catalog, with its number of published courses.
 * Kept apart from {@link CategoryResponse}, which is embedded in course responses and events.
 */
public record CategorySummaryView(
    String id,
    String name,
    long courseCount
) {
}
