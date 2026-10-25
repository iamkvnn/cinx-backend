-- cinx sells no free course (operator, 2026-10-25): FREE access becomes PURCHASE_ONLY, a free course gets a price, and
-- commerce hears the new price through the usual sellability event.

-- A price from a fixed scale, stable per course id. Drafts follow, or the next published change would bring 0 back.
UPDATE course c
LEFT JOIN course_access_policy p ON p.course_id = c.id
SET c.price = ELT(1 + MOD(CRC32(c.id), 5), 199000, 249000, 299000, 349000, 399000)
WHERE (p.access_model = 'FREE' OR c.access_model = 'FREE')
  AND COALESCE(c.price, 0) = 0;

UPDATE course_draft d
JOIN course c ON c.id = d.course_id
LEFT JOIN course_access_policy p ON p.course_id = c.id
SET d.price = c.price
WHERE (p.access_model = 'FREE' OR c.access_model = 'FREE')
  AND COALESCE(d.price, 0) = 0;

-- The same row CourseEventProducer#publishSellabilityChanged enqueues; the relay publishes it to commerce's projection.
INSERT INTO outbox_message
  (id, version, event_id, aggregate_type, aggregate_id, event_type, event_version, exchange_name, routing_key,
   payload_json, headers_json, status, attempts, next_attempt_at, created_at)
SELECT UUID(), 0, CONCAT('v36-sellability-', c.id), 'Course', c.id, 'CourseSellabilityChanged', 1,
       'cinx.domain.events', 'learning.course.sellability.changed',
       JSON_OBJECT(
         'eventId', CONCAT('v36-sellability-', c.id),
         'courseId', c.id,
         'title', c.title,
         'price', COALESCE(c.discounted_price, c.price),
         'categoryId', c.category_id,
         'instructorId', c.instructor_id,
         'sellable', IF(c.status = 'PUBLISHED', CAST('true' AS JSON), CAST('false' AS JSON)),
         'occurredAt', DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%f')),
       '{}', 'PENDING', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM course c
LEFT JOIN course_access_policy p ON p.course_id = c.id
WHERE p.access_model = 'FREE' OR c.access_model = 'FREE';

UPDATE course_access_policy
SET access_model = 'PURCHASE_ONLY',
    subscription_feature_key = NULL,
    effective_from = UTC_TIMESTAMP(6),
    changed_by = 'migration:v36',
    reason = 'FREE access removed; the course is sold'
WHERE access_model = 'FREE';

UPDATE course
SET access_model = 'PURCHASE_ONLY',
    subscription_feature_key = NULL
WHERE access_model = 'FREE';
