-- A create request may carry an Idempotency-Key.  Repeating the key with the same request returns the path created the
-- first time, so a client retrying after a lost response does not fail on the one-active-path rule.  Rows created
-- without a key keep NULL, which the unique key allows any number of times.
ALTER TABLE user_learning_path
  ADD COLUMN idempotency_key VARCHAR(255) NULL,
  ADD COLUMN request_fingerprint VARCHAR(64) NULL,
  ADD CONSTRAINT uk_learning_path_user_idempotency UNIQUE (user_id, idempotency_key);
