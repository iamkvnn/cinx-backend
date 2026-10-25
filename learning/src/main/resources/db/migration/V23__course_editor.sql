-- Course editor: an intro video on the course, an explicit order for course images, and an instructor-only
-- reference answer for essay quiz questions.

ALTER TABLE course
    ADD COLUMN promo_video_key       VARCHAR(512)  NULL,
    ADD COLUMN promo_video_url       VARCHAR(1024) NULL,
    ADD COLUMN promo_video_file_name VARCHAR(255)  NULL,
    ADD COLUMN promo_video_file_size BIGINT        NULL,
    ADD COLUMN promo_video_duration  INT           NULL;

-- CourseImage has no creation time; readers got primary-key order, so number the images per course by id.
ALTER TABLE course_image
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE course_image ci
    JOIN (SELECT id, ROW_NUMBER() OVER (PARTITION BY course_id ORDER BY id) - 1 AS rn FROM course_image) ranked
        ON ranked.id = ci.id
SET ci.sort_order = ranked.rn;

CREATE INDEX idx_course_image_course_order ON course_image (course_id, sort_order);

ALTER TABLE quiz_question
    ADD COLUMN reference_answer TEXT NULL;
