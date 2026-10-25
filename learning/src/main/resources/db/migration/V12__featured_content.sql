-- Admin-curated content for the public home and catalog pages:
-- featured learning paths (ordered courses), featured courses (home carousel) and featured reviews.

CREATE TABLE IF NOT EXISTS featured_learning_path (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  name VARCHAR(120) NOT NULL,
  role_title VARCHAR(120) NOT NULL,
  estimated_months INT NOT NULL,
  hours_per_week INT NOT NULL,
  sort_order INT NOT NULL,
  active BIT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_featured_path_active_order (active, sort_order)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS featured_learning_path_course (
  path_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (path_id, sort_order),
  KEY idx_featured_path_course_course (course_id),
  CONSTRAINT fk_featured_path_course_path FOREIGN KEY (path_id) REFERENCES featured_learning_path (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS featured_course (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  active BIT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_featured_course_course (course_id),
  KEY idx_featured_course_active_order (active, sort_order)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS featured_review (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  review_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  active BIT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_featured_review_review (review_id),
  KEY idx_featured_review_active_order (active, sort_order)
) ENGINE=InnoDB;
