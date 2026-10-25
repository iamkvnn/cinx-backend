package com.cinx.learning.catalog.application.command;

public record SyncQuizCommand(
        Boolean triggerRegrade,
        String changeReason
) {}

