-- Audited, idempotent local baseline. This intentionally seeds only the
-- internal free assignment; paid catalog offers remain disabled until a
-- provider publication job reconciles them.
INSERT IGNORE INTO plan_product_line
  (id, code, customer_segment, eligibility_policy_key, subscription_slot_code,
   display_order, status, created_at, updated_at)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'LEARNING', 'LEARNER', 'ACTIVE_LEARNER',
   'BASE', 0, 'ACTIVE', '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000');

INSERT IGNORE INTO billing_policy_version
  (id, code, version_number, publication_status, trial_enabled,
   payment_method_required, renewal_failure_policy, grace_duration_seconds,
   access_during_grace, quota_during_grace_policy, quota_rollover_enabled,
   quota_overage_enabled, upgrade_effective_policy, upgrade_proration_policy,
   upgrade_pending_update_required, downgrade_effective_policy,
   cancellation_effective_policy, eligibility_loss_access_policy,
   eligibility_loss_provider_policy, eligibility_loss_refund_policy)
VALUES
  ('00000000-0000-0000-0000-000000000102', 'STANDARD_NO_TRIAL', 1, 'PUBLISHED', 0,
   0, 'ENTER_GRACE', 259200, 1, 'DENY_METERED', 0, 0,
   'PROVIDER_PAYMENT_CONFIRMED', 'ALWAYS_INVOICE', 1, 'CURRENT_PERIOD_END',
   'CURRENT_PERIOD_END', 'IMMEDIATE_INELIGIBLE', 'CANCEL_AT_PERIOD_END',
   'MANUAL_REVIEW');

INSERT IGNORE INTO subscription_plan
  (id, product_line_id, code, lifecycle_status, created_at, updated_at)
VALUES
  ('00000000-0000-0000-0000-000000000103',
   '00000000-0000-0000-0000-000000000101', 'LEARNER_FREE', 'ACTIVE',
   '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000');

INSERT IGNORE INTO subscription_plan_version
  (id, plan_id, billing_policy_version_id, version_number, publication_status,
   default_locale, effective_from, published_at, published_by)
VALUES
  ('00000000-0000-0000-0000-000000000104',
   '00000000-0000-0000-0000-000000000103',
   '00000000-0000-0000-0000-000000000102', 1, 'PUBLISHED', 'vi-VN',
   '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000', 'bootstrap');

INSERT IGNORE INTO plan_offer
  (id, plan_version_id, plan_id, price_id, market_code, sales_start,
   sales_end, display_order, status)
VALUES
  ('00000000-0000-0000-0000-000000000105',
   '00000000-0000-0000-0000-000000000104',
   '00000000-0000-0000-0000-000000000103', NULL, 'VN',
   '2026-01-01 00:00:00.000000', NULL, 0, 'ACTIVE');

INSERT IGNORE INTO plan_entitlement
  (id, plan_version_id, feature_key, grant_type, boolean_value, quota_limit,
   quota_unlimited, quota_unit, reset_policy)
VALUES
  ('00000000-0000-0000-0000-000000000106', '00000000-0000-0000-0000-000000000104',
   'learning.subscription-catalog.access', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000107', '00000000-0000-0000-0000-000000000104',
   'learning.subscription-catalog.extended', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000108', '00000000-0000-0000-0000-000000000104',
   'learning.offline-download', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000109', '00000000-0000-0000-0000-000000000104',
   'learning.instructor.analytics.advanced', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000110', '00000000-0000-0000-0000-000000000104',
   'learning.instructor.bulk-tools', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000104',
   'learning.instructor.subtitle.minutes', 'QUOTA', NULL, 0, 0, 'MINUTE', 'CALENDAR_MONTH'),
  ('00000000-0000-0000-0000-000000000112', '00000000-0000-0000-0000-000000000104',
   'recommendation.agent.advanced', 'BOOLEAN', 0, NULL, NULL, NULL, NULL),
  ('00000000-0000-0000-0000-000000000113', '00000000-0000-0000-0000-000000000104',
   'recommendation.agent.requests', 'QUOTA', NULL, 350, 0, 'CREDIT', 'CALENDAR_MONTH'),
  ('00000000-0000-0000-0000-000000000114', '00000000-0000-0000-0000-000000000104',
   'recommendation.instructor.authoring.requests', 'QUOTA', NULL, 0, 0, 'REQUEST', 'CALENDAR_MONTH'),
  -- the instructor copilot's monthly credits (plan GĐ2-a); only the instructor persona spends them
  ('00000000-0000-0000-0000-000000000116', '00000000-0000-0000-0000-000000000104',
   'assistant.instructor.credits', 'QUOTA', NULL, 1500, 0, 'CREDIT', 'CALENDAR_MONTH');

INSERT IGNORE INTO plan_system_binding
  (id, binding_key, subject_type, source_product_line_id, subscription_slot_code,
   market_code, target_plan_version_id, effective_from, effective_until, status)
VALUES
  ('00000000-0000-0000-0000-000000000115', 'DEFAULT_USER_BASE', 'USER', NULL,
   'BASE', 'VN', '00000000-0000-0000-0000-000000000104',
   '2026-01-01 00:00:00.000000', NULL, 'ACTIVE');

-- The display copy of the free plan, so the public catalog shows a name and a description instead of the plan code.
INSERT IGNORE INTO plan_version_translation
  (plan_version_id, locale, display_name, description, badge)
VALUES
  ('00000000-0000-0000-0000-000000000104', 'vi-VN', 'Học viên Miễn phí',
   'Truy cập nền tảng cơ bản và hạn mức AI dùng thử hằng tháng.', NULL),
  ('00000000-0000-0000-0000-000000000104', 'en-US', 'Learner Free',
   'Basic access to the platform and a small monthly AI allowance.', NULL);
