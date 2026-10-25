CREATE TABLE IF NOT EXISTS answer_helpful_marks (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  answer_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_answer_helpful_marks_answer_user (answer_id, user_id),
  KEY idx_answer_helpful_marks_user (user_id)
) ENGINE=InnoDB;
