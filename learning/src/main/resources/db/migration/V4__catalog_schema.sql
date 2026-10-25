CREATE TABLE IF NOT EXISTS article_lesson (
  lesson_id VARCHAR(255) NOT NULL,
  article_url VARCHAR(255),
  file_key VARCHAR(255),
  file_name VARCHAR(255),
  file_type VARCHAR(255),
  file_size BIGINT,
  PRIMARY KEY (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignment_lesson (
  lesson_id VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  PRIMARY KEY (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignment_attachment (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  attachment_url VARCHAR(255),
  file_key VARCHAR(255),
  file_name VARCHAR(255),
  file_type VARCHAR(255),
  file_size BIGINT,
  assignment_lesson_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_assignment_attachment_lesson (assignment_lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS category (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  name VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  title VARCHAR(255),
  description VARCHAR(2000),
  duration BIGINT,
  price BIGINT,
  discounted_price BIGINT,
  discount_rate BIGINT,
  rating DOUBLE,
  enrollment_count BIGINT,
  is_in_subscription BIT,
  instructor_id VARCHAR(255),
  has_certificate BIT,
  certificate_title VARCHAR(255),
  status VARCHAR(255),
  publish_status VARCHAR(255),
  category_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_course_category (category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_draft (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255),
  title VARCHAR(255),
  description VARCHAR(2000),
  duration BIGINT,
  price BIGINT,
  discounted_price BIGINT,
  discount_rate BIGINT,
  is_in_subscription BIT,
  has_certificate BIT,
  certificate_title VARCHAR(255),
  category_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_course_draft_course (course_id),
  KEY idx_course_draft_category (category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_enrollment_event (
  id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_image (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  image_url VARCHAR(255),
  public_id VARCHAR(255),
  course_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_course_image_course (course_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS section (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  stable_id VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  description VARCHAR(255),
  duration BIGINT,
  order_index INT,
  course_id VARCHAR(255),
  draft_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_section_course (course_id),
  KEY idx_section_draft (draft_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lesson (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  stable_id VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  duration BIGINT,
  order_index INT,
  lesson_type TINYINT,
  is_preview BIT,
  section_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_lesson_section (section_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lesson_prerequisite_ids (
  lesson_id VARCHAR(255) NOT NULL,
  prerequisite_id VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_lesson (
  lesson_id VARCHAR(255) NOT NULL,
  duration INT,
  number_of_question_per_quiz_session INT,
  max_attempt INT,
  is_review_allowed BIT,
  is_show_answers_on_review BIT,
  shuffle_questions BIT,
  shuffle_options BIT,
  scoring_mode VARCHAR(255),
  has_pending_sync BIT,
  PRIMARY KEY (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_question (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  question_text VARCHAR(255),
  question_type VARCHAR(255),
  scoring_method VARCHAR(255),
  need_sync BIT,
  quiz_lesson_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_quiz_question_lesson (quiz_lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_option (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  option_text VARCHAR(255),
  is_correct BIT,
  option_order INT,
  match_text VARCHAR(255),
  quiz_question_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_quiz_option_question (quiz_question_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reject_course_reason (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255),
  reason VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subtitle_job (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  job_type VARCHAR(255) NOT NULL,
  status VARCHAR(255) NOT NULL,
  source_subtitle_id VARCHAR(255),
  output_subtitle_id VARCHAR(255),
  source_language_code VARCHAR(35),
  target_language_code VARCHAR(35) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  expected_output_file_key VARCHAR(255) NOT NULL,
  progress_percent INT NOT NULL,
  error_code VARCHAR(255),
  error_message TEXT,
  video_lesson_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_subtitle_job_video_status (video_lesson_id, status),
  KEY idx_subtitle_job_target_language (video_lesson_id, target_language_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subtitle_track (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  language_code VARCHAR(35) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(255) NOT NULL,
  file_key VARCHAR(255) NOT NULL,
  original_file_key VARCHAR(255),
  word_confidence_file_key VARCHAR(255),
  word_confidence_file_url VARCHAR(255),
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(255) NOT NULL,
  file_size BIGINT NOT NULL,
  format VARCHAR(255) NOT NULL,
  source VARCHAR(255) NOT NULL,
  status VARCHAR(255) NOT NULL,
  is_default BIT NOT NULL,
  video_lesson_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_subtitle_track_video_language (video_lesson_id, language_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS video_lesson (
  lesson_id VARCHAR(255) NOT NULL,
  video_url VARCHAR(255),
  file_key VARCHAR(255),
  file_name VARCHAR(255),
  file_type VARCHAR(255),
  file_size BIGINT,
  duration INT,
  status VARCHAR(255),
  PRIMARY KEY (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS video_question (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  question_text VARCHAR(255),
  question_type VARCHAR(255),
  timestamp_seconds INT,
  lesson_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_video_question_lesson (lesson_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS video_option (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  option_text VARCHAR(255),
  is_correct BIT,
  question_id VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_video_option_question (question_id)
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
  UNIQUE KEY uk_course_outbox_event_id (event_id),
  KEY idx_course_outbox_status_next (status, next_attempt_at)
) ENGINE=InnoDB;