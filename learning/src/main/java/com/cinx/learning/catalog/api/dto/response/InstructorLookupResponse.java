package com.cinx.learning.catalog.api.dto.response;

/** The one verified instructor behind an email, shown before an invitation is sent. */
public record InstructorLookupResponse(String instructorId, String name, String avatarUrl) {
}
