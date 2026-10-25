package com.cinx.user.profile.application.command;

/** An instructor application; {@code cvFileKey} names a document already uploaded for the profile. */
public record InstructorApplicationCommand(String cvFileKey) {
}
