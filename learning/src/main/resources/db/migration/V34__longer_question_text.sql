-- Quiz questions up to 2,000 characters (a code snippet or a short case); the session keeps a copy of the text.
ALTER TABLE quiz_question MODIFY question_text VARCHAR(2000);
ALTER TABLE quiz_session_question MODIFY question_text VARCHAR(2000);
