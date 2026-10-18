CREATE TABLE IF NOT EXISTS `user` (
  id VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  password VARCHAR(255),
  role TINYINT,
  status TINYINT,
  otp VARCHAR(255),
  otp_expire_at DATETIME(6),
  ban_expires_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS outbox_message (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  event_id VARCHAR(255) NOT NULL,
  aggregate_type VARCHAR(255) NOT NULL,
  aggregate_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  event_version INT NOT NULL,
  exchange_name VARCHAR(255) NOT NULL,
  routing_key VARCHAR(255) NOT NULL,
  payload_json TEXT NOT NULL,
  headers_json TEXT,
  status VARCHAR(255) NOT NULL,
  attempts INT NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  published_at DATETIME(6),
  last_error TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_outbox_event_id (event_id),
  KEY idx_auth_outbox_status_next (status, next_attempt_at)
) ENGINE=InnoDB;