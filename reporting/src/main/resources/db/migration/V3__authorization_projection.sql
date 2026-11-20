ALTER TABLE account_projection
  ADD COLUMN roles_json TEXT AFTER role,
  ADD COLUMN authorization_version BIGINT NOT NULL DEFAULT 0 AFTER roles_json,
  ADD COLUMN authorization_policy_version BIGINT NOT NULL DEFAULT 0 AFTER authorization_version;
