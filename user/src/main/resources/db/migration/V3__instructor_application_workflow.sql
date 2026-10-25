ALTER TABLE `user`
  ADD COLUMN instructor_application_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
  ADD COLUMN instructor_application_submitted_at DATETIME(6),
  ADD COLUMN instructor_application_reviewed_at DATETIME(6),
  ADD COLUMN instructor_application_rejection_reason VARCHAR(2000);
