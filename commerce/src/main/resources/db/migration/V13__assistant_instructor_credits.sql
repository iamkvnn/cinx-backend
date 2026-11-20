-- The instructor copilot's credits: the registry row R__subscription_bootstrap needs, whose INSERT IGNORE dropped the Free grant without it.
INSERT IGNORE INTO commerce_feature_registry
  (feature_key, owner_service, value_type, unit, reset_policies, saleable, active)
VALUES
  ('assistant.instructor.credits', 'assistant', 'QUOTA', 'CREDIT', 'CALENDAR_MONTH', 1, 1);

-- Learner Free: 1,500 credits a month; the repeatable bootstrap does not re-run on a database it already seeded.
INSERT IGNORE INTO plan_entitlement
  (id, plan_version_id, feature_key, grant_type, boolean_value, quota_limit,
   quota_unlimited, quota_unit, reset_policy)
SELECT '00000000-0000-0000-0000-000000000116', id,
       'assistant.instructor.credits', 'QUOTA', NULL, 1500, 0, 'CREDIT', 'CALENDAR_MONTH'
  FROM subscription_plan_version
 WHERE id = '00000000-0000-0000-0000-000000000104';
