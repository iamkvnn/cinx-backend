ALTER TABLE course
  MODIFY price DECIMAL(19,2),
  MODIFY discounted_price DECIMAL(19,2);

ALTER TABLE course_draft
  MODIFY price DECIMAL(19,2),
  MODIFY discounted_price DECIMAL(19,2);
