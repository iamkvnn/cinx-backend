-- Why the answer is right, shown to a learner only on review when the quiz shows answers; copied into each attempt.
ALTER TABLE quiz_question ADD COLUMN explanation TEXT NULL;
ALTER TABLE quiz_session_question ADD COLUMN explanation TEXT NULL;
