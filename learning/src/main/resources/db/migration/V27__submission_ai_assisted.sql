-- Whether the instructor used an AI grading suggestion; the learner sees it with the grade (plan GĐ2-b).
ALTER TABLE assignment_submission ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;
