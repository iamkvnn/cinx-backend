-- Each order line keeps who owned the course when it was sold, what the line brought in after the voucher, and the
-- platform fee percent of that time, so instructor revenue never shifts when ownership or the fee changes later.
ALTER TABLE commerce_order_item
  ADD COLUMN instructor_id VARCHAR(255) NULL,
  ADD COLUMN allocated_amount DECIMAL(19,2) NULL,
  ADD COLUMN platform_fee_percent DECIMAL(5,2) NULL;

-- Older lines: the owner at sale time was never stored, so today's owner stands in; the order's discount is split
-- by list price across all lines (which lines a voucher applied to was not stored either); the fee is 30%.
UPDATE commerce_order_item i
  JOIN commerce_order o ON o.id = i.order_id
  LEFT JOIN sellable_course c ON c.course_id = i.course_id
SET i.instructor_id = c.instructor_id,
    i.platform_fee_percent = 30.00,
    i.allocated_amount = CASE
        WHEN o.original_amount IS NULL OR o.original_amount = 0 THEN i.price_snapshot
        ELSE GREATEST(0, i.price_snapshot - ROUND(o.discount_amount * i.price_snapshot / o.original_amount, 2))
    END;

CREATE INDEX idx_commerce_order_item_instructor ON commerce_order_item (instructor_id);
