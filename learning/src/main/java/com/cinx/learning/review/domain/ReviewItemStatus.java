package com.cinx.learning.review.domain;

public enum ReviewItemStatus {
    /** In the queue: due on its date. */
    ACTIVE,
    /** Answered right in the last box; back to ACTIVE only when an official quiz gets it wrong again. */
    GRADUATED,
    /** No longer reviewable (question gone, answers now hidden, an essay); its attempts are kept. */
    RETIRED
}
