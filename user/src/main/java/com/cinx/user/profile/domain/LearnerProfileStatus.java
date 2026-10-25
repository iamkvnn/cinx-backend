package com.cinx.user.profile.domain;

/** Whether the learner has seen the profile questions; the week page invites them while it is NONE. */
public enum LearnerProfileStatus {
    /** Not asked yet, or only topics are known (a client older than the questions). */
    NONE,
    SKIPPED,
    COMPLETED
}
