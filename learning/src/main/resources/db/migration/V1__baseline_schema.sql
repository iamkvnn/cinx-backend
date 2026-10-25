CREATE TABLE IF NOT EXISTS assignment_submission (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  submission_time DATETIME(6),
  assignment_id VARCHAR(255),
  content VARCHAR(255),
  score DOUBLE,
  feedback VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignment_submission_attachment (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  attachment_url VARCHAR(255),
  file_key VARCHAR(255),
  file_name VARCHAR(255),
  file_type VARCHAR(255),
  file_size BIGINT,
  assignment_submission_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_assignment_submission_attachment_submission (assignment_submission_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS certificate_request (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  course_id VARCHAR(255),
  status VARCHAR(255),
  certificate_url VARCHAR(1000),
  requested_at DATETIME(6),
  approved_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_progress (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  course_id VARCHAR(255),
  is_completed BIT,
  is_passed BIT,
  avg_score DOUBLE,
  total_items INT,
  completed_items INT,
  completion_time DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_progress_user_course (user_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS in_video_assessment_submission (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  submission_time DATETIME(6),
  video_lesson_id VARCHAR(255),
  video_assessment_id VARCHAR(255),
  user_answer VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uk_in_video_assessment_submission_user_assessment (user_id, video_assessment_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_activity_daily (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  activity_date DATE NOT NULL,
  active_seconds BIGINT NOT NULL,
  last_activity_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_activity_daily_user_course_date (user_id, course_id, activity_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_item_progress (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  item_id VARCHAR(255) NOT NULL,
  is_completed BIT,
  is_passed BIT,
  score DOUBLE,
  course_progress_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_item_progress_course_item (course_progress_id, item_id),
  KEY idx_learning_item_progress_course_progress (course_progress_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_item_result (
  item_id VARCHAR(255) NOT NULL,
  score DOUBLE,
  weight INT,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_learning_path (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  title VARCHAR(255),
  description VARCHAR(2000),
  status VARCHAR(255),
  current_progress DOUBLE,
  total_items INT,
  completed_items INT,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_path_item (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  learning_path_id VARCHAR(255),
  course_id VARCHAR(255),
  lesson_id VARCHAR(255),
  order_index INT,
  is_suggested BIT,
  is_completed BIT,
  PRIMARY KEY (id),
  KEY idx_learning_path_item_path (learning_path_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_score_history (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  quiz_session_id VARCHAR(255),
  old_score DOUBLE,
  new_score DOUBLE,
  reason VARCHAR(255),
  graded_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_session (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  course_id VARCHAR(255),
  quiz_lesson_id VARCHAR(255),
  user_id VARCHAR(255),
  start_time DATETIME(6),
  end_time DATETIME(6),
  status TINYINT,
  is_review_allowed BIT,
  is_show_answers_on_review BIT,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_session_question (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  quiz_session_id VARCHAR(255),
  question_id VARCHAR(255),
  question_type VARCHAR(255),
  question_order INT,
  question_text VARCHAR(255),
  correct_answer VARCHAR(255),
  user_answer VARCHAR(255),
  scoring_method VARCHAR(255),
  score DOUBLE,
  options_snapshot TEXT,
  PRIMARY KEY (id),
  KEY idx_quiz_session_question_session (quiz_session_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_session_submission (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  submission_time DATETIME(6),
  quiz_session_id VARCHAR(255) NOT NULL,
  total_correct_answers INT,
  score DOUBLE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_session_submission_session (quiz_session_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submission_result (
  submission_id VARCHAR(255) NOT NULL,
  score DOUBLE,
  feedback VARCHAR(255),
  PRIMARY KEY (submission_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_daily_goal (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  goal_type VARCHAR(255) NOT NULL,
  goal_key VARCHAR(255) NOT NULL,
  target_value INT NOT NULL,
  current_value INT NOT NULL,
  target_item_id VARCHAR(255),
  goal_date DATE NOT NULL,
  is_completed BIT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_daily_goal_user_date_key (user_id, goal_date, goal_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_streak (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255),
  current_streak INT,
  highest_streak INT,
  last_activity_date DATE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_streak_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS video_lesson_tracking_history (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  video_lesson_id VARCHAR(255) NOT NULL,
  current_position INT NOT NULL,
  watched_ranges TEXT,
  last_tracking_time DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_lesson_tracking_user_lesson (user_id, video_lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS video_notes (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  lesson_id VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  video_timestamp INT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_video_note_user_lesson (user_id, lesson_id),
  KEY idx_video_note_user_course (user_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS outbox_message (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  event_id VARCHAR(255) NOT NULL,
  aggregate_type VARCHAR(255) NOT NULL,
  aggregate_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  event_version INT NOT NULL,
  exchange_name VARCHAR(255) NOT NULL,
  routing_key VARCHAR(255) NOT NULL,
  payload_json TEXT NOT NULL,
  headers_json TEXT,
  status VARCHAR(255) NOT NULL,
  attempts INT NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  published_at DATETIME(6),
  last_error TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_outbox_event_id (event_id),
  KEY idx_learning_outbox_status_next (status, next_attempt_at)
) ENGINE=InnoDB;