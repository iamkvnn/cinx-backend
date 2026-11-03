ALTER TABLE commerce_outbox
  ADD COLUMN exchange_name VARCHAR(160) NOT NULL DEFAULT 'cinx.domain.events',
  ADD COLUMN routing_key VARCHAR(180),
  ADD COLUMN event_version INT NOT NULL DEFAULT 1,
  ADD COLUMN headers_json LONGTEXT;

UPDATE commerce_outbox
SET routing_key = CASE event_type
  WHEN 'CommerceOrderPaid' THEN 'commerce.order.paid'
  WHEN 'CommerceWishlistAdded' THEN 'commerce.wishlist.added'
  WHEN 'CommerceWishlistRemoved' THEN 'commerce.wishlist.removed'
  ELSE 'commerce.event'
END
WHERE routing_key IS NULL;
