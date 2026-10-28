package com.cinx.learning.catalog.api.dto.response;

/** user's `/internal/users/instructor-lookup` answer. */
public record InstructorLookupDto(String id, String name, String avatarUrl) {
}
