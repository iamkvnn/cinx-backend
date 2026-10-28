package com.cinx.learning.courseview.api.dto.response;

public record InstructorResponse(
        String id,
        String name,
        String email,
        String bio,
        String profilePictureUrl
) {
}
