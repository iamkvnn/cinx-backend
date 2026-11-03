ALTER TABLE subscription_plan_version
  ADD COLUMN optimistic_version BIGINT NOT NULL DEFAULT 0;
