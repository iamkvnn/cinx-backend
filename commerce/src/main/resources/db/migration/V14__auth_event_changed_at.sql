-- auth.authorization.changed v3 orders by changedAt; the authorization version no longer exists.
ALTER TABLE commerce_identity_eligibility
  DROP COLUMN authorization_version,
  ADD COLUMN changed_at DATETIME(6) NOT NULL DEFAULT '1970-01-01 00:00:00';

ALTER TABLE auth_eligibility_event_inbox
  DROP COLUMN authorization_version;
