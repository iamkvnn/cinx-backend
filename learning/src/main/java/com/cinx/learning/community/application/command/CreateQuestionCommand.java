package com.cinx.learning.community.application.command;

import lombok.Data;

@Data
public class CreateQuestionCommand {
    private String courseId;

    private String lessonId;

    private String title;

    private String content;
}
