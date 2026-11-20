package com.cinx.learning.community.application.command;

import java.util.List;

/** One instructor answer posted to several questions ("Trả lời chung"): each question gets its own copy. */
public record BatchAnswerCommand(
          List< String> questionIds,
          String content,
          Boolean aiAssisted
) {
}
