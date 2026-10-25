-- Notes for every lesson type (Phase 3 player): video_notes becomes lesson_notes.
-- Existing rows keep their time, now position_seconds; it is null for non-video lessons.
RENAME TABLE video_notes TO lesson_notes;

ALTER TABLE lesson_notes
  CHANGE COLUMN video_timestamp position_seconds INT NULL,
  RENAME INDEX idx_video_note_user_lesson TO idx_lesson_note_user_lesson,
  RENAME INDEX idx_video_note_user_course TO idx_lesson_note_user_course;
