-- Orders are dated when created and can be cancelled while pending. Older paid orders take their payment time as their
-- creation time; older pending orders stay undated.
ALTER TABLE commerce_order
  ADD COLUMN created_at DATETIME(6) NULL,
  ADD COLUMN cancelled_at DATETIME(6) NULL;

UPDATE commerce_order SET created_at = paid_at WHERE created_at IS NULL AND paid_at IS NOT NULL;

CREATE INDEX idx_commerce_order_user_created ON commerce_order (user_id, created_at);
