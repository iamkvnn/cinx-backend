-- When a learner wants a study reminder; a learner without a row is reminded at the default hour.
CREATE TABLE study_reminder_preference (
    user_id       VARCHAR(255) NOT NULL PRIMARY KEY,
    enabled       BIT(1)       NOT NULL,
    reminder_hour INT          NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);

-- One study reminder per learner and day: the unique key stops a rerun or a second replica from sending again.
CREATE TABLE study_reminder_log (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    reminder_date DATE         NOT NULL,
    reason        VARCHAR(40)  NOT NULL,
    written_by    VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_study_reminder_log_user_date UNIQUE (user_id, reminder_date)
);
