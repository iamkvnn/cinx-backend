-- Records which policy document (and its version) a user agreed to, written when the profile is onboarded.
CREATE TABLE IF NOT EXISTS policy_acceptance (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  policy_id VARCHAR(255) NOT NULL,
  policy_version INT NOT NULL,
  accepted_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_policy_acceptance_user_policy (user_id, policy_id),
  KEY idx_policy_acceptance_policy (policy_id),
  CONSTRAINT fk_policy_acceptance_policy FOREIGN KEY (policy_id) REFERENCES policy_documents (id)
) ENGINE=InnoDB;
