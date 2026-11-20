CREATE TABLE subscription_subject (
  id VARCHAR(36) NOT NULL,
  subject_type VARCHAR(30) NOT NULL,
  subject_reference VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_subscription_subject (subject_type, subject_reference)
) ENGINE=InnoDB;

CREATE TABLE commerce_subscription (
  id VARCHAR(36) NOT NULL,
  subject_id VARCHAR(36) NOT NULL,
  plan_version_id VARCHAR(36) NOT NULL,
  price_id VARCHAR(36),
  assignment_binding_id VARCHAR(36),
  origin VARCHAR(30) NOT NULL,
  provider VARCHAR(40),
  provider_account_key VARCHAR(100),
  provider_customer_ref VARCHAR(255),
  provider_subscription_ref VARCHAR(255),
  billing_status VARCHAR(40) NOT NULL,
  access_status VARCHAR(40) NOT NULL,
  current_period_start DATETIME(6) NOT NULL,
  current_period_end DATETIME(6),
  trial_started_at DATETIME(6),
  trial_ends_at DATETIME(6),
  grace_started_at DATETIME(6),
  grace_ends_at DATETIME(6),
  cancel_at_period_end BIT NOT NULL DEFAULT 0,
  canceled_at DATETIME(6),
  ended_at DATETIME(6),
  aggregate_version BIGINT NOT NULL DEFAULT 1,
  optimistic_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_provider_subscription (provider, provider_account_key, provider_subscription_ref),
  CONSTRAINT fk_commerce_subscription_subject FOREIGN KEY (subject_id) REFERENCES subscription_subject(id),
  CONSTRAINT fk_commerce_subscription_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
  CONSTRAINT fk_commerce_subscription_price FOREIGN KEY (price_id) REFERENCES plan_price(id)
) ENGINE=InnoDB;

CREATE TABLE current_subject_subscription (
  subject_id VARCHAR(36) NOT NULL,
  subscription_slot_code VARCHAR(40) NOT NULL,
  subscription_id VARCHAR(36) NOT NULL,
  active_command_id VARCHAR(36),
  active_command_started_at DATETIME(6),
  optimistic_version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (subject_id, subscription_slot_code),
  UNIQUE KEY uk_current_subscription_id (subscription_id),
  CONSTRAINT fk_current_subscription_subject FOREIGN KEY (subject_id) REFERENCES subscription_subject(id),
  CONSTRAINT fk_current_subscription_subscription FOREIGN KEY (subscription_id) REFERENCES commerce_subscription(id)
) ENGINE=InnoDB;

CREATE TABLE subject_entitlement_state (
  subject_id VARCHAR(36) NOT NULL,
  entitlement_version BIGINT NOT NULL DEFAULT 0,
  snapshot_hash VARCHAR(128),
  optimistic_version BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (subject_id),
  CONSTRAINT fk_subject_entitlement_subject FOREIGN KEY (subject_id) REFERENCES subscription_subject(id)
) ENGINE=InnoDB;

CREATE TABLE subscription_command (
  command_id VARCHAR(36) NOT NULL,
  subject_id VARCHAR(36) NOT NULL,
  subscription_slot_code VARCHAR(40) NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL,
  request_fingerprint VARCHAR(128) NOT NULL,
  command_type VARCHAR(40) NOT NULL,
  provider_idempotency_key VARCHAR(255) NOT NULL,
  response_reference VARCHAR(255),
  status VARCHAR(40) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6),
  PRIMARY KEY (command_id),
  UNIQUE KEY uk_subscription_command_key (subject_id, idempotency_key),
  UNIQUE KEY uk_subscription_provider_command (provider_idempotency_key),
  CONSTRAINT fk_subscription_command_subject FOREIGN KEY (subject_id) REFERENCES subscription_subject(id)
) ENGINE=InnoDB;

CREATE TABLE subscription_scheduled_change (
  id VARCHAR(36) NOT NULL,
  subscription_id VARCHAR(36) NOT NULL,
  target_plan_version_id VARCHAR(36) NOT NULL,
  target_price_id VARCHAR(36),
  effective_at DATETIME(6) NOT NULL,
  status VARCHAR(30) NOT NULL,
  requested_by VARCHAR(255) NOT NULL,
  reason VARCHAR(1000),
  PRIMARY KEY (id),
  CONSTRAINT fk_scheduled_change_subscription FOREIGN KEY (subscription_id) REFERENCES commerce_subscription(id),
  CONSTRAINT fk_scheduled_change_plan FOREIGN KEY (target_plan_version_id) REFERENCES subscription_plan_version(id)
) ENGINE=InnoDB;

CREATE TABLE trial_redemption_claim (
  subject_id VARCHAR(36) NOT NULL,
  product_line_id VARCHAR(36) NOT NULL,
  subscription_id VARCHAR(36) NOT NULL,
  claimed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (subject_id, product_line_id),
  CONSTRAINT fk_trial_claim_subject FOREIGN KEY (subject_id) REFERENCES subscription_subject(id),
  CONSTRAINT fk_trial_claim_product_line FOREIGN KEY (product_line_id) REFERENCES plan_product_line(id),
  CONSTRAINT fk_trial_claim_subscription FOREIGN KEY (subscription_id) REFERENCES commerce_subscription(id)
) ENGINE=InnoDB;

CREATE TABLE subscription_change_quote (
  id VARCHAR(36) NOT NULL,
  command_id VARCHAR(36) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  provider_account_key VARCHAR(100) NOT NULL,
  provider_quote_ref VARCHAR(255),
  target_plan_version_id VARCHAR(36) NOT NULL,
  target_price_id VARCHAR(36),
  amount_due DECIMAL(19,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  currency_exponent INT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  quote_fingerprint VARCHAR(128) NOT NULL,
  accepted_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_subscription_quote_command (command_id),
  CONSTRAINT fk_subscription_quote_command FOREIGN KEY (command_id) REFERENCES subscription_command(command_id),
  CONSTRAINT fk_subscription_quote_plan FOREIGN KEY (target_plan_version_id) REFERENCES subscription_plan_version(id)
) ENGINE=InnoDB;

CREATE TABLE subscription_adjustment (
  id VARCHAR(36) NOT NULL,
  subscription_id VARCHAR(36) NOT NULL,
  feature_key VARCHAR(160),
  value_json JSON,
  effective_from DATETIME(6) NOT NULL,
  expires_at DATETIME(6),
  reason VARCHAR(1000) NOT NULL,
  created_by VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_subscription_adjustment_subscription FOREIGN KEY (subscription_id) REFERENCES commerce_subscription(id)
) ENGINE=InnoDB;

CREATE TABLE billing_webhook_inbox (
  provider VARCHAR(40) NOT NULL,
  provider_account_key VARCHAR(100) NOT NULL,
  provider_event_id VARCHAR(255) NOT NULL,
  payload_hash VARCHAR(128) NOT NULL,
  provider_object_ref VARCHAR(255),
  provider_object_version BIGINT,
  received_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6),
  processing_status VARCHAR(30) NOT NULL,
  failure_code VARCHAR(120),
  PRIMARY KEY (provider, provider_account_key, provider_event_id)
) ENGINE=InnoDB;

CREATE TABLE commerce_identity_eligibility (
  user_id VARCHAR(255) NOT NULL,
  account_status VARCHAR(40) NOT NULL,
  roles_json JSON NOT NULL,
  authorization_version BIGINT NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE auth_eligibility_event_inbox (
  event_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  authorization_version BIGINT NOT NULL,
  received_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id)
) ENGINE=InnoDB;

CREATE TABLE subscription_audit (
  id VARCHAR(36) NOT NULL,
  subscription_id VARCHAR(36),
  command_id VARCHAR(36),
  action VARCHAR(80) NOT NULL,
  actor_id VARCHAR(255),
  reason VARCHAR(1000),
  payload_hash VARCHAR(128),
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), KEY idx_subscription_audit (subscription_id, occurred_at)
) ENGINE=InnoDB;
