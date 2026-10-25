package com.cinx.user.instructor.api.dto.request;

/**
 * References a document already owned by the profile. Identity and roles are
 * always derived from the authenticated subject and the review workflow.
 */
public record InstructorApplicationRequest(String cvFileKey) {
}
