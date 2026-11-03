ALTER TABLE sellable_course
  MODIFY price DECIMAL(19,2) NOT NULL;

ALTER TABLE commerce_order
  MODIFY original_amount DECIMAL(19,2) NOT NULL,
  MODIFY discount_amount DECIMAL(19,2) NOT NULL,
  MODIFY total_amount DECIMAL(19,2) NOT NULL,
  MODIFY voucher_discount_value DECIMAL(19,2);

ALTER TABLE commerce_order_item
  MODIFY price_snapshot DECIMAL(19,2) NOT NULL;

ALTER TABLE payment
  MODIFY amount DECIMAL(19,2) NOT NULL;

ALTER TABLE voucher
  MODIFY discount_value DECIMAL(19,2) NOT NULL,
  MODIFY max_discount_amount DECIMAL(19,2),
  MODIFY minimum_eligible_amount DECIMAL(19,2) NOT NULL;

ALTER TABLE voucher_redemption
  MODIFY eligible_subtotal DECIMAL(19,2) NOT NULL,
  MODIFY discount_amount DECIMAL(19,2) NOT NULL;
