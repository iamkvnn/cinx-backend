CREATE TABLE IF NOT EXISTS notification (
  id VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  message VARCHAR(255),
  type VARCHAR(255),
  reference_id VARCHAR(255),
  action_url VARCHAR(255),
  metadata_json TEXT,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_notification (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255),
  notification_id VARCHAR(255),
  is_read BIT,
  sent_at DATETIME(6),
  PRIMARY KEY (id),
  KEY idx_user_notification_notification_id (notification_id),
  CONSTRAINT fk_user_notification_notification FOREIGN KEY (notification_id) REFERENCES notification (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inbox_message (
  message_id VARCHAR(255) NOT NULL,
  status VARCHAR(255),
  processed_at DATETIME(6),
  PRIMARY KEY (message_id)
) ENGINE=InnoDB;