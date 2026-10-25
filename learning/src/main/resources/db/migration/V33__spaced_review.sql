-- Spaced review (P3-03/P3-04): official quiz questions a learner missed come back on a Leitner schedule; review never touches official scores.
CREATE TABLE review_item (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    version          BIGINT       NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    course_id        VARCHAR(255) NOT NULL,
    lesson_id        VARCHAR(255) NOT NULL,
    question_id      VARCHAR(255) NOT NULL,
    box              INT          NOT NULL,
    due_on           DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    algorithm        VARCHAR(20)  NOT NULL,
    reason           VARCHAR(30)  NOT NULL,
    last_reviewed_at DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT uk_review_item_user_question UNIQUE (user_id, question_id),
    INDEX idx_review_item_due (user_id, status, due_on),
    INDEX idx_review_item_status_due (status, due_on)
);

CREATE TABLE review_attempt (
    id                  VARCHAR(36)   NOT NULL PRIMARY KEY,
    review_item_id      VARCHAR(36)   NOT NULL,
    user_id             VARCHAR(255)  NOT NULL,
    question_id         VARCHAR(255)  NOT NULL,
    user_answer         TEXT          NULL,
    score               DECIMAL(5, 4) NOT NULL,
    correct             BIT(1)        NOT NULL,
    box_before          INT           NOT NULL,
    box_after           INT           NOT NULL,
    due_on_after        DATE          NULL,
    answered_at         DATETIME(6)   NOT NULL,
    idempotency_key     VARCHAR(255)  NOT NULL,
    request_fingerprint VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_review_attempt_user_key UNIQUE (user_id, idempotency_key),
    INDEX idx_review_attempt_item (review_item_id)
);
