ALTER TABLE course_entitlement ADD COLUMN source_order_id VARCHAR(255);
CREATE UNIQUE INDEX uk_entitlement_order_course ON course_entitlement(source_order_id, course_id);
CREATE TABLE learning_inbox (
  event_id VARCHAR(255) NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id)
) ENGINE=InnoDB;
