CREATE TABLE learning_user_entitlement_projection (
  user_id VARCHAR(255) NOT NULL,
  subject_id VARCHAR(255) NOT NULL,
  source_summary_json JSON NOT NULL,
  entitlement_version BIGINT NOT NULL,
  access_status VARCHAR(40) NOT NULL,
  effective_from DATETIME(6) NOT NULL,
  effective_until DATETIME(6),
  reconcile_by DATETIME(6),
  snapshot_hash VARCHAR(128) NOT NULL,
  optimistic_version BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE learning_user_feature_entitlement (
  user_id VARCHAR(255) NOT NULL,
  feature_key VARCHAR(160) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  enabled BIT,
  quota_limit BIGINT,
  quota_unlimited BIT,
  quota_unit VARCHAR(40),
  reset_policy VARCHAR(40),
  quota_period_start DATETIME(6),
  quota_period_end DATETIME(6),
  effective_until DATETIME(6),
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, feature_key),
  CONSTRAINT fk_learning_feature_projection FOREIGN KEY (user_id) REFERENCES learning_user_entitlement_projection(user_id)
) ENGINE=InnoDB;

CREATE TABLE learning_entitlement_event_inbox (
  event_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  entitlement_version BIGINT NOT NULL,
  received_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id)
) ENGINE=InnoDB;
