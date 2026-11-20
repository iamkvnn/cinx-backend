ALTER TABLE commerce_subscription
  ADD COLUMN provider_object_version BIGINT NOT NULL DEFAULT 0;
