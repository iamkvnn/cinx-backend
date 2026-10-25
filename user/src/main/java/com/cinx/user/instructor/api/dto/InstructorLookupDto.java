package com.cinx.user.instructor.api.dto;

/** The one verified instructor behind an email, for a course owner inviting a co-instructor. */
public record InstructorLookupDto(String id, String name, String avatarUrl) {
}
