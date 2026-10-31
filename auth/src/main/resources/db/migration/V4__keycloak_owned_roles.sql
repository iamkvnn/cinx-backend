-- Keycloak owns realm roles; auth keeps only status, ban and the time of the last authorization change.
DROP TABLE keycloak_sync_job;
DROP TABLE identity_account_role;

ALTER TABLE identity_account
  DROP INDEX uk_identity_account_keycloak_subject,
  DROP INDEX idx_identity_account_email,
  DROP COLUMN keycloak_subject,
  DROP COLUMN email,
  DROP COLUMN role,
  DROP COLUMN sync_status,
  DROP COLUMN authorization_version,
  DROP COLUMN authorization_policy_version,
  ADD COLUMN changed_at DATETIME(6) NULL AFTER status;
