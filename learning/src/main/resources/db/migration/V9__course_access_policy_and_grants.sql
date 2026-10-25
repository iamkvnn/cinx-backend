CREATE TABLE IF NOT EXISTS course_access_policy (
  course_id VARCHAR(255) NOT NULL,
  access_model VARCHAR(32) NOT NULL,
  subscription_feature_key VARCHAR(160),
  effective_from DATETIME(6) NOT NULL,
  effective_until DATETIME(6),
  changed_by VARCHAR(255) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (course_id),
  KEY idx_course_access_policy_model (access_model)
) ENGINE=InnoDB;

-- The policy is authoritative after cutover, while these additive columns keep
-- course events backward compatible during the v2 event rollout.
ALTER TABLE course
  ADD COLUMN access_model VARCHAR(32),
  ADD COLUMN subscription_feature_key VARCHAR(160);

CREATE TABLE IF NOT EXISTS course_access_grant (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  source VARCHAR(24) NOT NULL,
  source_reference VARCHAR(255) NOT NULL,
  valid_from DATETIME(6) NOT NULL,
  valid_until DATETIME(6),
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  -- source_reference is prefix-indexed: the full four-column key exceeds InnoDB's 3072-byte limit under utf8mb4.
  UNIQUE KEY uk_course_access_grant_source (course_id, user_id, source, source_reference(191)),
  KEY idx_course_access_grant_user_course (user_id, course_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_enrollment (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  enrolled_at DATETIME(6) NOT NULL,
  access_source_at_enrollment VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_enrollment_user_course (user_id, course_id),
  KEY idx_course_enrollment_course (course_id)
) ENGINE=InnoDB;

-- Backfill policies without changing the legacy is_in_subscription column.
INSERT IGNORE INTO course_access_policy
  (course_id, access_model, subscription_feature_key, effective_from, changed_by, reason, version)
SELECT c.id,
       CASE
         WHEN COALESCE(c.price, 0) = 0 THEN 'FREE'
         WHEN c.is_in_subscription = 1 THEN 'PURCHASE_OR_SUBSCRIPTION'
         ELSE 'PURCHASE_ONLY'
       END,
       CASE WHEN c.is_in_subscription = 1 THEN 'learning.subscription-catalog.access' ELSE NULL END,
       COALESCE(c.created_at, UTC_TIMESTAMP(6)), 'migration:v9', 'Legacy course access backfill', 0
FROM course c;

UPDATE course c
JOIN course_access_policy p ON p.course_id = c.id
SET c.access_model = p.access_model,
    c.subscription_feature_key = p.subscription_feature_key;

-- Existing permanent entitlements become permanent purchase/admin grants.
INSERT IGNORE INTO course_access_grant
  (id, user_id, course_id, source, source_reference, valid_from, status)
SELECT CONCAT('purchase:', e.id), e.user_id, e.course_id,
       CASE WHEN e.source_order_id IS NULL THEN 'ADMIN' ELSE 'PURCHASE' END,
       COALESCE(e.source_order_id, e.id), e.granted_at, 'ACTIVE'
FROM course_entitlement e;

INSERT IGNORE INTO course_enrollment
  (id, user_id, course_id, enrolled_at, access_source_at_enrollment, status)
SELECT CONCAT('legacy:', e.user_id, ':', e.course_id), e.user_id, e.course_id,
       e.granted_at, CASE WHEN e.source_order_id IS NULL THEN 'ADMIN' ELSE 'PURCHASE' END, 'ACTIVE'
FROM course_entitlement e;
