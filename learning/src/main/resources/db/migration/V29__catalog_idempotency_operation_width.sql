-- An operation names its target ("quiz-question.create:<courseId>:<lessonId>" is 94 characters), which 64 could not hold.
ALTER TABLE catalog_idempotency_record MODIFY operation VARCHAR(160) NOT NULL;
