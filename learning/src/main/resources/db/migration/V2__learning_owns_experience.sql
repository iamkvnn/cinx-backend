CREATE TABLE user_experience (
  user_id VARCHAR(255) NOT NULL,
  xp INT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;
