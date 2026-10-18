CREATE TABLE IF NOT EXISTS `user` (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  email VARCHAR(255),
  name VARCHAR(255),
  gender TINYINT,
  role TINYINT,
  is_instructor_verified BIT,
  status TINYINT,
  avatar_file_key VARCHAR(255),
  avatar_url VARCHAR(255),
  phone_number VARCHAR(255),
  bio VARCHAR(2000),
  last_access_at DATETIME(6),
  instructor_verified_at DATETIME(6),
  partnership_terminated_at DATETIME(6),
  partnership_termination_reason_type VARCHAR(255),
  partnership_termination_reason_detail VARCHAR(2000),
  is_receive_push_notification BOOLEAN DEFAULT FALSE,
  is_partnership_terminated BOOLEAN DEFAULT FALSE,
  xp INT,
  user_id VARCHAR(255),
  cv_file_key VARCHAR(255),
  cv_url VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_device_token (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  fcm_token VARCHAR(255) NOT NULL,
  device_info VARCHAR(255),
  updated_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_device_token_fcm_token (fcm_token)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS policy_documents (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  policy_type VARCHAR(30) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  title VARCHAR(255) NOT NULL,
  summary VARCHAR(1000),
  status VARCHAR(30) NOT NULL,
  version_number INT NOT NULL,
  effective_at DATETIME(6),
  published_at DATETIME(6),
  display_order INT,
  PRIMARY KEY (id),
  KEY idx_policy_slug_status (slug, status),
  KEY idx_policy_type_status (policy_type, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS policy_sections (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  document_id VARCHAR(255) NOT NULL,
  heading VARCHAR(255) NOT NULL,
  anchor VARCHAR(255) NOT NULL,
  body_markdown TEXT NOT NULL,
  order_index INT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_policy_section_document (document_id),
  CONSTRAINT fk_policy_sections_document FOREIGN KEY (document_id) REFERENCES policy_documents (id)
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
  UNIQUE KEY uk_user_outbox_event_id (event_id),
  KEY idx_user_outbox_status_next (status, next_attempt_at)
) ENGINE=InnoDB;