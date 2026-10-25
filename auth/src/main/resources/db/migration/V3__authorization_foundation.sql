ALTER TABLE identity_account
  ADD COLUMN authorization_version BIGINT NOT NULL DEFAULT 1 AFTER sync_status,
  ADD COLUMN authorization_policy_version BIGINT NOT NULL DEFAULT 1 AFTER authorization_version;

CREATE TABLE identity_account_role (
  user_id VARCHAR(255) NOT NULL,
  role_code VARCHAR(80) NOT NULL,
  granted_by VARCHAR(255) NOT NULL,
  granted_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6),
  status VARCHAR(20) NOT NULL,
  revoked_at DATETIME(6),
  source VARCHAR(80) NOT NULL,
  PRIMARY KEY (user_id, role_code),
  KEY idx_identity_role_status_expiry (status, expires_at),
  CONSTRAINT fk_identity_role_account FOREIGN KEY (user_id) REFERENCES identity_account(user_id)
) ENGINE=InnoDB;

INSERT INTO identity_account_role
  (user_id, role_code, granted_by, granted_at, status, source)
SELECT user_id,
       CASE WHEN role = 'INSTRUCTOR' THEN 'INSTRUCTOR'
            ELSE 'LEARNER' END,
       'migration-v3', created_at, 'ACTIVE', 'LEGACY_MIGRATION'
FROM identity_account;

INSERT IGNORE INTO identity_account_role
  (user_id, role_code, granted_by, granted_at, status, source)
SELECT user_id, 'LEARNER', 'migration-v3', created_at, 'ACTIVE', 'LEGACY_MIGRATION'
FROM identity_account WHERE role = 'INSTRUCTOR';

ALTER TABLE identity_account MODIFY COLUMN role VARCHAR(40) NULL;

CREATE TABLE authorization_audit (
  id VARCHAR(36) NOT NULL,
  actor_type VARCHAR(40) NOT NULL,
  actor_id VARCHAR(255) NOT NULL,
  target_user_id VARCHAR(255) NOT NULL,
  action VARCHAR(80) NOT NULL,
  role_code VARCHAR(80),
  before_json TEXT,
  after_json TEXT NOT NULL,
  authorization_policy_version BIGINT NOT NULL,
  request_id VARCHAR(255),
  trace_id VARCHAR(64),
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_authorization_audit_target_time (target_user_id, occurred_at)
) ENGINE=InnoDB;

CREATE TABLE keycloak_sync_job (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  desired_authorization_version BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6) NOT NULL,
  last_error_code VARCHAR(255),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_keycloak_sync_user_version (user_id, desired_authorization_version),
  KEY idx_keycloak_sync_status_next (status, next_attempt_at),
  CONSTRAINT fk_keycloak_sync_account FOREIGN KEY (user_id) REFERENCES identity_account(user_id)
) ENGINE=InnoDB;

CREATE TABLE auth_event_inbox (
  event_id VARCHAR(255) NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id)
) ENGINE=InnoDB;
