-- The learner's "Thống kê sử dụng" switch (P0-07): whether product events about them may be recorded; on by default.
ALTER TABLE learner_profile ADD COLUMN usage_analytics_consent BIT(1) NOT NULL DEFAULT 1;
