-- What the learner has already been shown about their streak: the highest milestone celebrated (7, 30, 100) and the
-- last active day of a streak whose loss was shown. No backfill: there is no learner data yet.

ALTER TABLE user_streak
  ADD COLUMN celebrated_milestone INT NOT NULL DEFAULT 0,
  ADD COLUMN lost_noticed_on DATE NULL;
