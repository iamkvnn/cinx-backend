-- auth.authorization.changed v3 orders by changedAt; the authorization versions no longer exist.
ALTER TABLE account_projection
  DROP COLUMN authorization_version,
  DROP COLUMN authorization_policy_version,
  ADD COLUMN changed_at DATETIME(6) NULL AFTER roles_json;
