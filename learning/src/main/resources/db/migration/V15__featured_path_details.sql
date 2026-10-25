-- Public path page (/paths/:id): an admin-entered description and "after this path you can" outcomes per featured path.

ALTER TABLE featured_learning_path ADD COLUMN description VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS featured_learning_path_outcome (
  path_id VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  outcome VARCHAR(120) NOT NULL,
  PRIMARY KEY (path_id, sort_order),
  CONSTRAINT fk_featured_path_outcome_path FOREIGN KEY (path_id) REFERENCES featured_learning_path (id) ON DELETE CASCADE
) ENGINE=InnoDB;
