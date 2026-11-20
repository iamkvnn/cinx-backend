CREATE TABLE IF NOT EXISTS reporting_entitlement_projection (
  user_id VARCHAR(255) NOT NULL PRIMARY KEY,
  entitlement_version BIGINT NOT NULL,
  access_status VARCHAR(32) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS reporting_entitlement_feature_projection (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  feature_key VARCHAR(160) NOT NULL,
  enabled BIT NOT NULL,
  quota_limit BIGINT,
  entitlement_version BIGINT NOT NULL,
  UNIQUE KEY uk_reporting_entitlement_feature (user_id, feature_key)
) ENGINE=InnoDB;
