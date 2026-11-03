-- One row per payment link handed to a customer for a pending order; the provider reference (Stripe client_reference_id,
-- MoMo orderId, VNPay vnp_TxnRef) is how a signed webhook finds its way back to the order. `payment` stays the paid ledger.
CREATE TABLE payment_attempt (
  id VARCHAR(255) NOT NULL,
  order_id VARCHAR(255) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  provider_reference VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  payment_url TEXT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_payment_attempt_reference UNIQUE (provider, provider_reference),
  KEY idx_payment_attempt_order (order_id, provider, status)
) ENGINE=InnoDB;
