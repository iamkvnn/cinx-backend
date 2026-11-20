package com.cinx.learning.community.application.command;

public record CreateReviewCommand(
        String courseId,

        String content,

        Double rating
) {
}
