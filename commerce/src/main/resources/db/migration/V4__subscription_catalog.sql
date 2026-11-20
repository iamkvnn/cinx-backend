CREATE TABLE commerce_feature_registry (
  feature_key VARCHAR(160) NOT NULL,
  owner_service VARCHAR(80) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  unit VARCHAR(40),
  reset_policies VARCHAR(160),
  saleable BIT NOT NULL DEFAULT 0,
  active BIT NOT NULL DEFAULT 1,
  PRIMARY KEY (feature_key)
) ENGINE=InnoDB;

CREATE TABLE currency_exponent_registry (
  currency_code CHAR(3) NOT NULL,
  exponent_value INT NOT NULL,
  active BIT NOT NULL DEFAULT 1,
  PRIMARY KEY (currency_code)
) ENGINE=InnoDB;

INSERT IGNORE INTO currency_exponent_registry (currency_code, exponent_value, active)
VALUES ('VND', 0, 1), ('JPY', 0, 1), ('KRW', 0, 1),
       ('USD', 2, 1), ('EUR', 2, 1);

INSERT IGNORE INTO commerce_feature_registry
  (feature_key, owner_service, value_type, unit, reset_policies, saleable, active)
VALUES
 ('learning.subscription-catalog.access', 'learning', 'BOOLEAN', NULL, NULL, 1, 1),
 ('learning.subscription-catalog.extended', 'learning', 'BOOLEAN', NULL, NULL, 0, 1),
 ('learning.offline-download', 'learning', 'BOOLEAN', NULL, NULL, 0, 1),
 ('learning.instructor.analytics.advanced', 'learning', 'BOOLEAN', NULL, NULL, 0, 1),
 ('learning.instructor.bulk-tools', 'learning', 'BOOLEAN', NULL, NULL, 0, 1),
 ('learning.instructor.subtitle.minutes', 'learning', 'QUOTA', 'MINUTE', 'CALENDAR_MONTH', 0, 1),
 ('recommendation.agent.advanced', 'recommendation', 'BOOLEAN', NULL, NULL, 1, 1),
 ('recommendation.agent.requests', 'recommendation', 'QUOTA', 'REQUEST', 'CALENDAR_MONTH,SUBSCRIPTION_PERIOD', 1, 1),
 ('recommendation.instructor.authoring.requests', 'recommendation', 'QUOTA', 'REQUEST', 'CALENDAR_MONTH', 0, 1);

CREATE TABLE plan_product_line (
  id VARCHAR(36) NOT NULL,
  code VARCHAR(40) NOT NULL,
  customer_segment VARCHAR(40) NOT NULL,
  eligibility_policy_key VARCHAR(100) NOT NULL,
  subscription_slot_code VARCHAR(40) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_plan_product_line_code (code)
) ENGINE=InnoDB;

CREATE TABLE billing_policy_version (
  id VARCHAR(36) NOT NULL,
  code VARCHAR(80) NOT NULL,
  version_number BIGINT NOT NULL,
  publication_status VARCHAR(20) NOT NULL,
  trial_enabled BIT NOT NULL DEFAULT 0,
  trial_duration_days INT,
  payment_method_required BIT NOT NULL DEFAULT 0,
  maximum_trial_redemptions INT,
  trial_redemption_scope VARCHAR(40),
  renewal_failure_policy VARCHAR(40) NOT NULL,
  grace_duration_seconds BIGINT NOT NULL DEFAULT 0,
  access_during_grace BIT NOT NULL DEFAULT 0,
  quota_during_grace_policy VARCHAR(40) NOT NULL,
  quota_rollover_enabled BIT NOT NULL DEFAULT 0,
  quota_overage_enabled BIT NOT NULL DEFAULT 0,
  upgrade_effective_policy VARCHAR(60) NOT NULL,
  upgrade_proration_policy VARCHAR(60) NOT NULL,
  upgrade_pending_update_required BIT NOT NULL DEFAULT 0,
  downgrade_effective_policy VARCHAR(60) NOT NULL,
  cancellation_effective_policy VARCHAR(60) NOT NULL,
  eligibility_loss_access_policy VARCHAR(60) NOT NULL,
  eligibility_loss_provider_policy VARCHAR(60) NOT NULL,
  eligibility_loss_refund_policy VARCHAR(60) NOT NULL,
  published_at DATETIME(6),
  PRIMARY KEY (id), UNIQUE KEY uk_billing_policy_version (code, version_number)
) ENGINE=InnoDB;

CREATE TABLE subscription_plan (
  id VARCHAR(36) NOT NULL,
  product_line_id VARCHAR(36) NOT NULL,
  code VARCHAR(80) NOT NULL,
  lifecycle_status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_subscription_plan_code (code),
  CONSTRAINT fk_subscription_plan_product_line FOREIGN KEY (product_line_id) REFERENCES plan_product_line(id)
) ENGINE=InnoDB;

CREATE TABLE subscription_plan_version (
  id VARCHAR(36) NOT NULL,
  plan_id VARCHAR(36) NOT NULL,
  billing_policy_version_id VARCHAR(36) NOT NULL,
  version_number BIGINT NOT NULL,
  publication_status VARCHAR(20) NOT NULL,
  default_locale VARCHAR(20) NOT NULL,
  effective_from DATETIME(6),
  published_at DATETIME(6),
  published_by VARCHAR(255),
  PRIMARY KEY (id), UNIQUE KEY uk_subscription_plan_version (plan_id, version_number),
  CONSTRAINT fk_plan_version_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),
  CONSTRAINT fk_plan_version_policy FOREIGN KEY (billing_policy_version_id) REFERENCES billing_policy_version(id)
) ENGINE=InnoDB;

CREATE TABLE plan_version_translation (
  plan_version_id VARCHAR(36) NOT NULL,
  locale VARCHAR(20) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  description VARCHAR(2000),
  badge VARCHAR(100),
  PRIMARY KEY (plan_version_id, locale),
  CONSTRAINT fk_plan_translation_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id)
) ENGINE=InnoDB;

CREATE TABLE plan_provider_product (
  id VARCHAR(36) NOT NULL,
  plan_id VARCHAR(36) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  provider_account_key VARCHAR(100) NOT NULL,
  provider_product_ref VARCHAR(255),
  provider_lookup_key VARCHAR(255),
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_provider_product (plan_id, provider, provider_account_key),
  UNIQUE KEY uk_provider_product_ref (provider, provider_account_key, provider_product_ref),
  CONSTRAINT fk_provider_product_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id)
) ENGINE=InnoDB;

CREATE TABLE plan_price (
  id VARCHAR(36) NOT NULL,
  plan_id VARCHAR(36) NOT NULL,
  provider_product_id VARCHAR(36),
  market_code CHAR(2) NOT NULL,
  currency CHAR(3) NOT NULL,
  billing_interval VARCHAR(20) NOT NULL,
  interval_count INT NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  provider VARCHAR(40),
  provider_account_key VARCHAR(100),
  provider_price_ref VARCHAR(255),
  provider_lookup_key VARCHAR(255),
  tax_behavior VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_provider_price_ref (provider, provider_account_key, provider_price_ref),
  CONSTRAINT fk_plan_price_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),
  CONSTRAINT fk_plan_price_product FOREIGN KEY (provider_product_id) REFERENCES plan_provider_product(id)
) ENGINE=InnoDB;

CREATE TABLE plan_offer (
  id VARCHAR(36) NOT NULL,
  plan_version_id VARCHAR(36) NOT NULL,
  plan_id VARCHAR(36) NOT NULL,
  price_id VARCHAR(36),
  market_code CHAR(2) NOT NULL,
  sales_start DATETIME(6),
  sales_end DATETIME(6),
  display_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_offer_position (plan_version_id, market_code, display_order),
  CONSTRAINT fk_plan_offer_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
  CONSTRAINT fk_plan_offer_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),
  CONSTRAINT fk_plan_offer_price FOREIGN KEY (price_id) REFERENCES plan_price(id)
) ENGINE=InnoDB;

CREATE TABLE plan_entitlement (
  id VARCHAR(36) NOT NULL,
  plan_version_id VARCHAR(36) NOT NULL,
  feature_key VARCHAR(160) NOT NULL,
  grant_type VARCHAR(20) NOT NULL,
  boolean_value BIT,
  quota_limit BIGINT,
  quota_unlimited BIT,
  quota_unit VARCHAR(40),
  reset_policy VARCHAR(40),
  configuration_json JSON,
  PRIMARY KEY (id), UNIQUE KEY uk_plan_entitlement_feature (plan_version_id, feature_key),
  CONSTRAINT fk_plan_entitlement_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
  CONSTRAINT fk_plan_entitlement_feature FOREIGN KEY (feature_key) REFERENCES commerce_feature_registry(feature_key)
) ENGINE=InnoDB;

CREATE TABLE plan_system_binding (
  id VARCHAR(36) NOT NULL,
  binding_key VARCHAR(100) NOT NULL,
  subject_type VARCHAR(30) NOT NULL,
  source_product_line_id VARCHAR(36),
  subscription_slot_code VARCHAR(40) NOT NULL,
  market_code CHAR(2) NOT NULL,
  target_plan_version_id VARCHAR(36) NOT NULL,
  effective_from DATETIME(6) NOT NULL,
  effective_until DATETIME(6),
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_system_binding (binding_key, subject_type, source_product_line_id, subscription_slot_code, market_code, effective_from),
  CONSTRAINT fk_system_binding_product_line FOREIGN KEY (source_product_line_id) REFERENCES plan_product_line(id),
  CONSTRAINT fk_system_binding_version FOREIGN KEY (target_plan_version_id) REFERENCES subscription_plan_version(id)
) ENGINE=InnoDB;

CREATE TABLE plan_publication_job (
  id VARCHAR(36) NOT NULL,
  plan_version_id VARCHAR(36) NOT NULL,
  command_id VARCHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6),
  last_error_code VARCHAR(120),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_plan_publication_command (command_id),
  CONSTRAINT fk_plan_publication_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id)
) ENGINE=InnoDB;

CREATE TABLE commerce_catalog_audit (
  id VARCHAR(36) NOT NULL,
  aggregate_type VARCHAR(80) NOT NULL,
  aggregate_id VARCHAR(36) NOT NULL,
  action VARCHAR(80) NOT NULL,
  actor_id VARCHAR(255) NOT NULL,
  reason VARCHAR(1000),
  payload_hash VARCHAR(128),
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), KEY idx_catalog_audit_aggregate (aggregate_type, aggregate_id, occurred_at)
) ENGINE=InnoDB;
