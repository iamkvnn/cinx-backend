CREATE TABLE IF NOT EXISTS course_instructor_membership (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  course_id VARCHAR(255) NOT NULL,
  instructor_id VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  invited_by VARCHAR(255),
  invited_at DATETIME(6),
  accepted_at DATETIME(6),
  revoked_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_instructor_membership (course_id, instructor_id),
  KEY idx_membership_instructor_status (instructor_id, status),
  KEY idx_membership_course_status (course_id, status)
) ENGINE=InnoDB;

INSERT IGNORE INTO course_instructor_membership
    (id, version, created_at, updated_at, created_by, updated_by,
     course_id, instructor_id, role, status, invited_by, invited_at, accepted_at)
SELECT UUID(), 0, c.created_at, c.updated_at, c.created_by, c.updated_by,
       c.id, c.instructor_id, 'OWNER', 'ACTIVE', c.instructor_id,
       COALESCE(c.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(c.created_at, CURRENT_TIMESTAMP(6))
FROM course c
WHERE c.instructor_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM course_instructor_membership m
      WHERE m.course_id = c.id
        AND m.instructor_id = c.instructor_id
  );
