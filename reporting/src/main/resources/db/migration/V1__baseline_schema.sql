CREATE TABLE account_projection (
  user_id VARCHAR(255) NOT NULL,
  status VARCHAR(40),
  role VARCHAR(40),
  updated_at DATETIME(6),
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE order_projection (
  order_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255),
  total_price BIGINT,
  discounted BIGINT,
  status VARCHAR(40),
  order_date DATETIME(6),
  updated_at DATETIME(6),
  PRIMARY KEY (order_id),
  KEY idx_reporting_order_status (status)
) ENGINE=InnoDB;

CREATE TABLE enrollment_projection (
  id VARCHAR(511) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  granted_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_reporting_enrollment_course (course_id)
) ENGINE=InnoDB;

CREATE TABLE inbox_message (
  message_id VARCHAR(255) NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (message_id)
) ENGINE=InnoDB;
