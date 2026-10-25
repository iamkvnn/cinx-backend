CREATE TABLE identity_account (
  user_id VARCHAR(255) NOT NULL,
  keycloak_subject VARCHAR(255),
  email VARCHAR(320),
  role VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL,
  sync_status VARCHAR(40) NOT NULL,
  ban_reason VARCHAR(100),
  ban_details VARCHAR(2000),
  ban_expires_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_identity_account_keycloak_subject (keycloak_subject),
  KEY idx_identity_account_email (email),
  KEY idx_identity_account_status_ban_expires (status, ban_expires_at)
) ENGINE=InnoDB;
