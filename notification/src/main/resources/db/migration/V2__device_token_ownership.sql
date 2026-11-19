CREATE TABLE user_device_token (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  fcm_token VARCHAR(512) NOT NULL,
  device_info VARCHAR(255),
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_device_token (fcm_token),
  KEY idx_notification_device_token_user (user_id)
) ENGINE=InnoDB;
