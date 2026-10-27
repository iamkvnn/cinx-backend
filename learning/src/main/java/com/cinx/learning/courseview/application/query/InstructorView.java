package com.cinx.learning.courseview.application.query;

public record InstructorView(
        String id,
        String name,
        String email,
        String bio,
        String profilePictureUrl
) {
}
