package com.cinx.commerce.catalog.application.command;

/** A new draft version of a plan; the plan and its product line are created when they do not exist yet. */
public record CreatePlanDraftCommand(String code, String productLine, String defaultLocale) {
}
