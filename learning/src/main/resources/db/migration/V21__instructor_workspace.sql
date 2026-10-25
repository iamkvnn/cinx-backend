-- Instructor workspace: grading with criteria, line comments and saved comments, the inbox activity log,
-- per-course certificate conditions, and located notes when a course is sent back by review.

-- Who graded a submission and when; feedback was never written and was too short to hold a real comment.
ALTER TABLE assignment_submission
  ADD COLUMN graded_at DATETIME(6) NULL,
  ADD COLUMN graded_by VARCHAR(255) NULL,
  MODIFY COLUMN feedback TEXT NULL;

CREATE INDEX idx_assignment_submission_assignment_graded ON assignment_submission (assignment_id, graded_at);

-- The criteria of an assignment; their points add up to 10 (checked on save).
CREATE TABLE IF NOT EXISTS assignment_criterion (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  assignment_id VARCHAR(255) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  max_points DOUBLE NOT NULL,
  order_index INT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_assignment_criterion_assignment (assignment_id, order_index)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submission_criterion_score (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  submission_id VARCHAR(255) NOT NULL,
  criterion_id VARCHAR(255) NOT NULL,
  points DOUBLE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_submission_criterion_score (submission_id, criterion_id),
  KEY idx_submission_criterion_score_criterion (criterion_id)
) ENGINE=InnoDB;

-- A bonus or a penalty outside the criteria, always with its reason.
CREATE TABLE IF NOT EXISTS submission_grade_adjustment (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  submission_id VARCHAR(255) NOT NULL,
  points DOUBLE NOT NULL,
  reason VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_submission_grade_adjustment_submission (submission_id)
) ENGINE=InnoDB;

-- A comment on one line of a submitted text file.
CREATE TABLE IF NOT EXISTS submission_line_comment (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  submission_id VARCHAR(255) NOT NULL,
  attachment_id VARCHAR(255) NOT NULL,
  line INT NOT NULL,
  content TEXT NOT NULL,
  author_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_submission_line_comment_submission (submission_id)
) ENGINE=InnoDB;

-- Comments an instructor saved to reuse, per kind of work.
CREATE TABLE IF NOT EXISTS instructor_comment_template (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  instructor_id VARCHAR(255) NOT NULL,
  kind VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_instructor_comment_template_owner (instructor_id, kind)
) ENGINE=InnoDB;

-- What an instructor did from the inbox ("Đã xử lý"), one row per action, however many items it covered.
CREATE TABLE IF NOT EXISTS instructor_activity (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  instructor_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  kind VARCHAR(30) NOT NULL,
  action VARCHAR(30) NOT NULL,
  target_id VARCHAR(255) NULL,
  item_count INT NOT NULL,
  summary VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_instructor_activity_owner_time (instructor_id, created_at),
  KEY idx_instructor_activity_course_time (course_id, created_at)
) ENGINE=InnoDB;

-- A course's certificate conditions; a course without a row uses the defaults (100%, 5, 5, no auto-approve).
CREATE TABLE IF NOT EXISTS course_certificate_conditions (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255) NOT NULL,
  min_completion_percent INT NOT NULL,
  min_quiz_score DOUBLE NOT NULL,
  min_assignment_score DOUBLE NOT NULL,
  auto_approve BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_certificate_conditions_course (course_id)
) ENGINE=InnoDB;

-- A reviewer's note on one place of a course, drafted while reviewing and sent with the rejection.
CREATE TABLE IF NOT EXISTS course_review_note (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255) NOT NULL,
  target VARCHAR(30) NOT NULL,
  lesson_id VARCHAR(255) NULL,
  video_second INT NULL,
  content TEXT NOT NULL,
  status VARCHAR(30) NOT NULL,
  sent_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_course_review_note_course_status (course_id, status)
) ENGINE=InnoDB;

-- The overall message of a rejection is optional now that notes can carry it, and may be longer.
ALTER TABLE reject_course_reason
  MODIFY COLUMN reason TEXT NULL;
