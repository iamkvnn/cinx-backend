package com.cinx.user.policy.application.command;

public record PolicySectionCommand(String heading, String anchor, String bodyMarkdown, Integer orderIndex) {
}
