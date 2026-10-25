-- "Tuần của tôi": a learner's study plan (free weekdays + minutes per day). Lessons are placed on days when the plan is
-- read, so only the settings, the chosen courses and the days marked busy are stored.

CREATE TABLE IF NOT EXISTS study_plan (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  user_id VARCHAR(255) NOT NULL,
  source VARCHAR(16) NOT NULL,
  path_id VARCHAR(255) NULL,
  minutes_per_day INT NOT NULL,
  free_days VARCHAR(7) NOT NULL,
  start_date DATE NOT NULL,
  baseline_finish_date DATE NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_study_plan_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS study_plan_course (
  plan_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (plan_id, sort_order),
  CONSTRAINT fk_study_plan_course_plan FOREIGN KEY (plan_id) REFERENCES study_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS study_plan_day_off (
  plan_id VARCHAR(255) NOT NULL,
  day_off DATE NOT NULL,
  PRIMARY KEY (plan_id, day_off),
  CONSTRAINT fk_study_plan_day_off_plan FOREIGN KEY (plan_id) REFERENCES study_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- When an item first became complete (UTC); null for items completed before this migration.
ALTER TABLE learning_item_progress ADD COLUMN completed_at DATETIME(6) NULL;

-- The lesson the latest heartbeat was about, for "Học tiếp".
ALTER TABLE learning_activity_daily ADD COLUMN last_item_id VARCHAR(255) NULL;
