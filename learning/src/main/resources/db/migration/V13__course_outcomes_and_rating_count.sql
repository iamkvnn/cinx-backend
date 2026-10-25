-- Course outcomes ("what you will be able to do", up to 5 short lines), edited on the draft and
-- copied to the published course on approval, plus a stored review count next to the average rating.

CREATE TABLE IF NOT EXISTS course_outcome (
  course_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  outcome VARCHAR(120) NOT NULL,
  PRIMARY KEY (course_id, sort_order),
  CONSTRAINT fk_course_outcome_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_draft_outcome (
  course_draft_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  outcome VARCHAR(120) NOT NULL,
  PRIMARY KEY (course_draft_id, sort_order),
  CONSTRAINT fk_course_draft_outcome_draft FOREIGN KEY (course_draft_id) REFERENCES course_draft (id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE course ADD COLUMN rating_count BIGINT NOT NULL DEFAULT 0;

UPDATE course c
SET c.rating_count = (SELECT COUNT(*) FROM review r WHERE r.course_id = c.id);
