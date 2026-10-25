-- Whether an AI draft helped write the content; the learner sees a label. The client declares it and it is only ever turned on.
ALTER TABLE course_answers ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE quiz_question ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE quiz_session_question ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE course ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE course_draft ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
