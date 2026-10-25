package com.cinx.learning.catalog.domain;

public enum SubtitleStatus {
    /** Learners see it: uploaded by a person, or AI output an instructor approved. */
    READY,
    PROCESSING,
    FAILED,
    /** AI output that learners do not see until an instructor approves it. */
    REVIEW_REQUIRED
}
