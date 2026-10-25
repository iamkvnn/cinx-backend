-- Pool of teaser questions for the public home page. One question is shown per day, rotating
-- through the active questions by sort_order (see DailyQuizService).

CREATE TABLE IF NOT EXISTS daily_quiz_question (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  topic VARCHAR(60) NOT NULL,
  prompt VARCHAR(500) NOT NULL,
  code_snippet TEXT,
  explanation VARCHAR(1000) NOT NULL,
  answer_key VARCHAR(1) NOT NULL,
  sort_order INT NOT NULL,
  active BIT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_daily_quiz_active_order (active, sort_order)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS daily_quiz_option (
  question_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  option_key VARCHAR(1) NOT NULL,
  option_text VARCHAR(200) NOT NULL,
  PRIMARY KEY (question_id, sort_order),
  CONSTRAINT fk_daily_quiz_option_question FOREIGN KEY (question_id) REFERENCES daily_quiz_question (id) ON DELETE CASCADE
) ENGINE=InnoDB;
