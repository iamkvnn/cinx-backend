package com.cinx.learning.assessment.application.command;

public record LineCommentCommand(
         String attachmentId,
          Integer line,
          String content
) {
}
