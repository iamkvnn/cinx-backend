package com.cinx.user.profile.application.query;

/** The one verified instructor behind an email, for a course owner inviting a co-instructor. */
public record InstructorLookup(String id, String name, String avatarUrl) {
}
