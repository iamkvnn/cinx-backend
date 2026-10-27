package com.cinx.learning.community.application.command;

import lombok.Data;

@Data
public class UpdateQuestionCommand {
    private String title;

    private String content;
}
