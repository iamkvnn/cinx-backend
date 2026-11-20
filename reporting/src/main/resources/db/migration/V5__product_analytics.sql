-- Product events, experiments and the analytics consent projection (P0-07); raw events are kept 180 days, daily counts longer.
CREATE TABLE product_event (
    user_id     VARCHAR(255) NOT NULL,
    event_id    CHAR(36)     NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    received_at DATETIME(6)  NOT NULL,
    props       JSON         NULL,
    PRIMARY KEY (user_id, event_id),
    KEY idx_product_event_name_time (name, occurred_at),
    KEY idx_product_event_time (occurred_at)
);

CREATE TABLE product_event_daily (
    day    DATE        NOT NULL,
    name   VARCHAR(64) NOT NULL,
    events BIGINT      NOT NULL,
    users  BIGINT      NOT NULL,
    PRIMARY KEY (day, name)
);

CREATE TABLE experiment (
    experiment_key   VARCHAR(64)   NOT NULL,
    version          BIGINT        NOT NULL,
    description      VARCHAR(500)  NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    variants         VARCHAR(1000) NOT NULL,
    salt             VARCHAR(64)   NOT NULL,
    goal_event       VARCHAR(64)   NOT NULL,
    goal_filter      VARCHAR(1000) NOT NULL,
    goal_window_days INT           NOT NULL,
    started_at       DATETIME(6)   NULL,
    stopped_at       DATETIME(6)   NULL,
    updated_by       VARCHAR(255)  NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (experiment_key)
);

CREATE TABLE experiment_exposure (
    experiment_key VARCHAR(64)  NOT NULL,
    user_id        VARCHAR(255) NOT NULL,
    variant        VARCHAR(64)  NOT NULL,
    exposed_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (experiment_key, user_id),
    KEY idx_experiment_exposure_variant (experiment_key, variant)
);

CREATE TABLE analytics_consent (
    user_id    VARCHAR(255) NOT NULL,
    enabled    BIT(1)       NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id)
);
