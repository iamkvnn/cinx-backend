-- What a learner is learning for, their level, daily time, language and topics: one row per learner (P1-06).
CREATE TABLE IF NOT EXISTS learner_profile (
  user_id VARCHAR(255) NOT NULL,
  goal VARCHAR(30) NULL,
  level VARCHAR(20) NULL,
  minutes_per_day INT NULL,
  content_language VARCHAR(10) NULL,
  personalization_consent BIT(1) NOT NULL,
  status VARCHAR(20) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learner_profile_category (
  user_id VARCHAR(255) NOT NULL,
  category_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (user_id, category_id),
  CONSTRAINT fk_learner_profile_category_profile FOREIGN KEY (user_id) REFERENCES learner_profile (user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
