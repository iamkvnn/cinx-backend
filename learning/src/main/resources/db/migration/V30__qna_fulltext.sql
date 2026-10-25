-- Lets a learner writing a question see the ones already asked; the collation makes the match ignore accents.
ALTER TABLE course_questions ADD FULLTEXT INDEX ft_course_q_title_content (title, content);
