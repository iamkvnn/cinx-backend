-- Idempotency-Key of catalog creates (sections, lessons, quiz questions): a retried request returns what the first one made.
CREATE TABLE catalog_idempotency_record (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(64)  NOT NULL,
    idem_key      VARCHAR(255) NOT NULL,
    operation     VARCHAR(64)  NOT NULL,
    request_hash  CHAR(64)     NOT NULL,
    resource_id   VARCHAR(64)  NOT NULL,
    -- the first response, returned as-is to a retry
    response_json MEDIUMTEXT   NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_catalog_idempotency_user_key UNIQUE (user_id, idem_key)
);
