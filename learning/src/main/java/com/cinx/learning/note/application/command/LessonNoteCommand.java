package com.cinx.learning.note.application.command;

/** Create or update a lesson note; positionSeconds only for video lessons. */
public record LessonNoteCommand(
        String content,
        Integer positionSeconds
) {
}
