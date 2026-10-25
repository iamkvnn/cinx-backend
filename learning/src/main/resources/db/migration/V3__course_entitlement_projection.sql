CREATE TABLE course_entitlement (
  id VARCHAR(511) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255) NOT NULL,
  granted_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_entitlement_user_course (user_id, course_id),
  KEY idx_entitlement_course (course_id)
) ENGINE=InnoDB;
