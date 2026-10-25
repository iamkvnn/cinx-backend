ALTER TABLE `user`
  ADD COLUMN onboarding_idempotency_key VARCHAR(255),
  ADD COLUMN onboarding_request_fingerprint VARCHAR(64),
  ADD CONSTRAINT uk_user_onboarding_idempotency
    UNIQUE (user_id, onboarding_idempotency_key);
