package com.cinx.learning.catalog.application.query;

/** The one verified instructor behind an email, shown before an invitation is sent. */
public record InstructorLookupViewX(String instructorId, String name, String avatarUrl) {
}
