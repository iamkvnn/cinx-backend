CREATE TABLE commerce_order (
  id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL, total_amount BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL, paid_at DATETIME(6), PRIMARY KEY (id)
) ENGINE=InnoDB;
CREATE TABLE commerce_order_item (
  id VARCHAR(255) NOT NULL, order_id VARCHAR(255) NOT NULL, course_id VARCHAR(255) NOT NULL,
  title_snapshot VARCHAR(500) NOT NULL, price_snapshot BIGINT NOT NULL, PRIMARY KEY (id),
  CONSTRAINT uk_order_course UNIQUE (order_id, course_id),
  CONSTRAINT fk_commerce_item_order FOREIGN KEY (order_id) REFERENCES commerce_order(id)
) ENGINE=InnoDB;
CREATE TABLE payment (
  id VARCHAR(255) NOT NULL, order_id VARCHAR(255) NOT NULL, provider VARCHAR(80) NOT NULL,
  provider_event_id VARCHAR(255) NOT NULL, amount BIGINT NOT NULL, paid_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id), CONSTRAINT uk_payment_provider_event UNIQUE (provider, provider_event_id)
) ENGINE=InnoDB;
CREATE TABLE commerce_outbox (
  event_id VARCHAR(255) NOT NULL, event_type VARCHAR(120) NOT NULL, aggregate_id VARCHAR(255) NOT NULL,
  payload LONGTEXT NOT NULL, created_at DATETIME(6) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME(6), published_at DATETIME(6),
  last_error VARCHAR(1000), PRIMARY KEY (event_id),
  KEY idx_commerce_outbox_pending (status, next_attempt_at, created_at)
) ENGINE=InnoDB;
CREATE TABLE commerce_inbox (
  event_id VARCHAR(255) NOT NULL, processed_at DATETIME(6) NOT NULL, PRIMARY KEY (event_id)
) ENGINE=InnoDB;
CREATE TABLE sellable_course (
  course_id VARCHAR(255) NOT NULL, title VARCHAR(500) NOT NULL, price BIGINT NOT NULL,
  sellable BIT NOT NULL, updated_at DATETIME(6) NOT NULL, PRIMARY KEY (course_id)
) ENGINE=InnoDB;
CREATE TABLE cart_item (
  id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL, course_id VARCHAR(255) NOT NULL,
  added_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
  CONSTRAINT uk_cart_user_course UNIQUE (user_id, course_id)
) ENGINE=InnoDB;
CREATE TABLE wishlist_item (
  id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL, course_id VARCHAR(255) NOT NULL,
  added_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
  CONSTRAINT uk_wishlist_user_course UNIQUE (user_id, course_id)
) ENGINE=InnoDB;
CREATE TABLE voucher (
  id VARCHAR(255) NOT NULL, code VARCHAR(120) NOT NULL, discount_amount BIGINT NOT NULL,
  minimum_amount BIGINT NOT NULL, remaining_uses BIGINT NOT NULL, valid_from DATETIME(6) NOT NULL,
  valid_to DATETIME(6) NOT NULL, active BIT NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_voucher_code (code)
) ENGINE=InnoDB;
