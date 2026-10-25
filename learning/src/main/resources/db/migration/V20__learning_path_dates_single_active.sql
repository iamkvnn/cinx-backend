-- Learning paths get their dates, and the one-active-path rule moves into the database.
--
-- Dates: rows written before this migration keep NULL; the service sets them from now on.
ALTER TABLE user_learning_path
  ADD COLUMN created_at DATETIME(6) NULL,
  ADD COLUMN completed_at DATETIME(6) NULL,
  ADD COLUMN dropped_at DATETIME(6) NULL;

-- The rule "one ACTIVE or PENDING_PAYMENT path per user" was only a read-then-insert check, so concurrent creates
-- could leave two.  Keep one per user (the one with the most items, then the highest id) and drop the rest, so the
-- unique key below can be built.  A dropped row keeps its items and progress; only its status changes.
UPDATE user_learning_path p
JOIN (
  SELECT id FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY COALESCE(total_items, 0) DESC, id DESC) AS rn
    FROM user_learning_path
    WHERE status IN ('ACTIVE', 'PENDING_PAYMENT')
  ) ranked
  WHERE ranked.rn > 1
) extra ON extra.id = p.id
SET p.status = 'DROPPED';

-- NULL for every other status, and a unique key allows any number of NULLs.
ALTER TABLE user_learning_path
  ADD COLUMN active_user_id VARCHAR(255)
    GENERATED ALWAYS AS (CASE WHEN status IN ('ACTIVE', 'PENDING_PAYMENT') THEN user_id END) STORED,
  ADD CONSTRAINT uk_learning_path_one_active UNIQUE (active_user_id),
  ADD INDEX idx_learning_path_user (user_id, created_at);
