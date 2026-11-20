package com.cinx.learning.reminder.domain;

/** Why a learner is reminded today, in priority order: one reminder carries the first that applies. */
public enum ReminderReason {
    PLAN_SESSION_PENDING,
    STREAK_AT_RISK,
    /** Spaced-review questions are due today. */
    REVIEWS_DUE,
    DAILY_GOAL_PENDING,
    INACTIVE
}
