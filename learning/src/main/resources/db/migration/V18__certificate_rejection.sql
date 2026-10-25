-- A refused certificate request keeps the instructor's reason, and when it was refused, so the learner can see why
-- and apply again (the request row is reopened as PENDING).
ALTER TABLE certificate_request
  ADD COLUMN rejection_reason VARCHAR(500) NULL,
  ADD COLUMN rejected_at DATETIME(6) NULL;
