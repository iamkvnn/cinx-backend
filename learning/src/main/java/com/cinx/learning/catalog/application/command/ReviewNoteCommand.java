package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.ReviewNoteTarget;

/** A reviewer's note on one place of a course; lessonId is the lesson's stable id and required for LESSON. */
public record ReviewNoteCommand(
         ReviewNoteTarget target,
        String lessonId,
         Integer videoSecond,
          String content
) {
}
