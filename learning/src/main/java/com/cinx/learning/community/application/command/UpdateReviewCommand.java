package com.cinx.learning.community.application.command;

public record UpdateReviewCommand(
        String content,

        Double rating
) {
}
