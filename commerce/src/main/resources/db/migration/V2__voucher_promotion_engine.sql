ALTER TABLE sellable_course ADD COLUMN category_id VARCHAR(255), ADD COLUMN instructor_id VARCHAR(255);
ALTER TABLE commerce_order ADD COLUMN original_amount BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN discount_amount BIGINT NOT NULL DEFAULT 0, ADD COLUMN voucher_code VARCHAR(120),
  ADD COLUMN voucher_discount_type VARCHAR(20), ADD COLUMN voucher_discount_value BIGINT,
  ADD COLUMN idempotency_key VARCHAR(255), ADD COLUMN request_fingerprint VARCHAR(64);
UPDATE commerce_order SET original_amount = total_amount, idempotency_key = CONCAT('legacy-', id),
  request_fingerprint = SHA2(CONCAT('legacy-', id), 256);
ALTER TABLE commerce_order MODIFY idempotency_key VARCHAR(255) NOT NULL,
  MODIFY request_fingerprint VARCHAR(64) NOT NULL,
  ADD CONSTRAINT uk_order_user_idempotency UNIQUE (user_id, idempotency_key);
ALTER TABLE voucher ADD COLUMN discount_type VARCHAR(20), ADD COLUMN discount_value BIGINT,
  ADD COLUMN max_discount_amount BIGINT, ADD COLUMN minimum_eligible_amount BIGINT,
  ADD COLUMN minimum_eligible_items INT NOT NULL DEFAULT 0, ADD COLUMN total_usage_limit BIGINT,
  ADD COLUMN used_count BIGINT NOT NULL DEFAULT 0, ADD COLUMN per_user_limit BIGINT,
  ADD COLUMN first_purchase_only BIT NOT NULL DEFAULT 0;
UPDATE voucher SET discount_type = 'FIXED', discount_value = discount_amount,
  minimum_eligible_amount = minimum_amount, total_usage_limit = remaining_uses, per_user_limit = remaining_uses;
ALTER TABLE voucher MODIFY discount_type VARCHAR(20) NOT NULL, MODIFY discount_value BIGINT NOT NULL,
  MODIFY minimum_eligible_amount BIGINT NOT NULL, MODIFY total_usage_limit BIGINT NOT NULL,
  MODIFY per_user_limit BIGINT NOT NULL, DROP COLUMN discount_amount,
  DROP COLUMN minimum_amount, DROP COLUMN remaining_uses;
CREATE TABLE voucher_target_rule (
  id VARCHAR(255) NOT NULL, voucher_id VARCHAR(255) NOT NULL, target_type VARCHAR(20) NOT NULL,
  match_type VARCHAR(20) NOT NULL, target_id VARCHAR(255) NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_voucher_target_rule (voucher_id, target_type, match_type, target_id),
  CONSTRAINT fk_voucher_target_rule FOREIGN KEY (voucher_id) REFERENCES voucher(id)
) ENGINE=InnoDB;
CREATE TABLE voucher_redemption (
  id VARCHAR(255) NOT NULL, voucher_id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL,
  order_id VARCHAR(255) NOT NULL, eligible_subtotal BIGINT NOT NULL, discount_amount BIGINT NOT NULL,
  consumed_at DATETIME(6) NOT NULL, first_purchase_claim BIT NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_redemption_order (order_id), UNIQUE KEY uk_redemption_voucher_order (voucher_id, order_id),
  KEY idx_redemption_voucher_user (voucher_id, user_id),
  CONSTRAINT fk_redemption_voucher FOREIGN KEY (voucher_id) REFERENCES voucher(id),
  CONSTRAINT fk_redemption_order FOREIGN KEY (order_id) REFERENCES commerce_order(id)
) ENGINE=InnoDB;
CREATE TABLE first_purchase_voucher_claim (
  user_id VARCHAR(255) NOT NULL, redemption_id VARCHAR(255) NOT NULL, claimed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id), UNIQUE KEY uk_first_purchase_redemption (redemption_id),
  CONSTRAINT fk_first_purchase_redemption FOREIGN KEY (redemption_id) REFERENCES voucher_redemption(id)
) ENGINE=InnoDB;
