-- When a course was first published, last submitted for review, and archived.
-- No backfill: existing courses keep NULL and the instructor list shows "—" (operator: "khoá cũ để trống").
ALTER TABLE course
    ADD COLUMN published_at DATETIME(6) NULL,
    ADD COLUMN submitted_at DATETIME(6) NULL,
    ADD COLUMN archived_at  DATETIME(6) NULL;
