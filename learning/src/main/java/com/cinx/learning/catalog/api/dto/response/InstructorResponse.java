package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

public record InstructorResponse(
        @Schema(example = "usr_123")
        String id,
        @Schema(example = "Nguyen Van A")
        String name,
        @Schema(example = "instructor@gmail.com")
        String email,
        @Schema(example = "MALE")
        Gender gender,
        @Schema(example = "https://example.com/profiles/123.jpg")
        String avatarUrl,
        @Schema(description = "Public bio from the user profile", example = "Tech Lead backend, 11 years.")
        String bio
) {
    /** Public reads never carry the instructor's email. */
    public InstructorResponse withoutEmail() {
        return new InstructorResponse(id, name, null, gender, avatarUrl, bio);
    }

    /** The recommendation event keeps its documented shape, which has no bio. */
    public InstructorResponse withoutBio() {
        return new InstructorResponse(id, name, email, gender, avatarUrl, null);
    }
}

