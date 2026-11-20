package com.cinx.learning.community.application.command;

import lombok.Data;

@Data
public class CreateAnswerCommand {
    private String questionId;

    private String parentAnswerId;

    private String content;

    private Boolean aiAssisted;
}
