package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.Gender;

public record InstructorView(
        String id,
        String name,
        String email,
        Gender gender,
        String avatarUrl,
        String bio
) {
    /** Public reads never carry the instructor's email. */
    public InstructorView withoutEmail() {
        return new InstructorView(id, name, null, gender, avatarUrl, bio);
    }

    /** The recommendation event keeps its documented shape, which has no bio. */
    public InstructorView withoutBio() {
        return new InstructorView(id, name, email, gender, avatarUrl, null);
    }
}

