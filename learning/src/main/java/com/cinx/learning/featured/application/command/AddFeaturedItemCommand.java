package com.cinx.learning.featured.application.command;

/** Adds a course (featured courses) or a review (featured reviews) to the end of the featured list. */
public record AddFeaturedItemCommand(
        String targetId
) {
}
