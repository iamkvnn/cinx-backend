CREATE TABLE IF NOT EXISTS answer_upvotes (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  answer_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_answer_upvotes_answer_user (answer_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_answers (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  question_id VARCHAR(255) NOT NULL,
  parent_answer_id VARCHAR(255),
  user_id VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  is_instructor_answer BOOLEAN NOT NULL DEFAULT FALSE,
  upvote_count INT NOT NULL DEFAULT 0,
  depth INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_course_a_question (question_id, parent_answer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_questions (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255) NOT NULL,
  lesson_id VARCHAR(255),
  user_id VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  upvote_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_course_q_course (course_id),
  KEY idx_course_q_lesson (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_upvotes (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  question_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_upvotes_question_user (question_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reports (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  reporter_id VARCHAR(255) NOT NULL,
  ref_id VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  reason TEXT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_report_ref_type (ref_id, type)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  user_id VARCHAR(255),
  course_id VARCHAR(255),
  rating DOUBLE,
  content VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_user_course (user_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review_reaction (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255),
  review_id VARCHAR(255),
  liked BIT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review_replies (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  review_id VARCHAR(255) NOT NULL,
  instructor_id VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_replies_review_id (review_id)
) ENGINE=InnoDB;

