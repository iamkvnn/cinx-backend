import os
import unittest
from unittest.mock import patch


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")
os.environ.setdefault("AWS_S3_CDN_URL", "https://cdn.test")
os.environ.setdefault("AWS_REGION", "test-region")
os.environ.setdefault("RABBITMQ_USER", "guest")
os.environ.setdefault("RABBITMQ_PASSWORD", "guest")
os.environ.setdefault("DIGITALOCEAN_LLM_MODEL", "test-model")

from app.messaging.rabbitmq import RejectMessage, SubtitleEventPublisher, publish_failure_from_body


class FakePublisher:
    def __init__(self):
        self.failed = None

    async def publish_failed(self, job_id, error_code, error_message):
        self.failed = {
            "jobId": job_id,
            "errorCode": error_code,
            "errorMessage": error_message,
        }


class FlakyExchange:
    def __init__(self):
        self.attempts = 0
        self.messages = []

    async def publish(self, message, routing_key, mandatory=False):
        self.attempts += 1
        if self.attempts == 1:
            raise ConnectionError("connection lost")
        self.messages.append((routing_key, mandatory, message))


class BrokenPublisher:
    async def publish_failed(self, job_id, error_code, error_message):
        raise ConnectionError("connection lost")


class MessagingTest(unittest.IsolatedAsyncioTestCase):
    async def test_failed_event_shape(self):
        publisher = FakePublisher()
        await publish_failure_from_body(b'{"jobId":"job-1"}', publisher, RuntimeError("boom"))
        self.assertEqual("job-1", publisher.failed["jobId"])
        self.assertEqual("AI_SUBTITLE_FAILED", publisher.failed["errorCode"])
        self.assertIn("boom", publisher.failed["errorMessage"])

    async def test_publish_retries_after_connection_error(self):
        exchange = FlakyExchange()
        publisher = SubtitleEventPublisher(exchange)

        with patch("app.messaging.rabbitmq.asyncio.sleep", return_value=None):
            await publisher.publish_progress("job-1", 70)

        self.assertEqual(2, exchange.attempts)
        self.assertEqual("ai.subtitle.job.progress", exchange.messages[0][0])
        self.assertTrue(exchange.messages[0][1])
        message = exchange.messages[0][2]
        self.assertEqual("job-1", message.headers["aggregateId"])
        self.assertEqual("SubtitleJobProgress", message.headers["eventType"])
        self.assertEqual(1, message.headers["eventVersion"])
        self.assertEqual(message.message_id, message.headers["eventId"])
        self.assertIn("occurredAt", message.headers)

    async def test_failure_publish_connection_error_rejects_to_dlq(self):
        # Learning would never hear about the job, so the request must reach the DLQ instead of being acked.
        with self.assertRaises(RejectMessage) as caught:
            await publish_failure_from_body(b'{"jobId":"job-1"}', BrokenPublisher(), RuntimeError("boom"))
        self.assertEqual("publish_failed", caught.exception.reason)


if __name__ == "__main__":
    unittest.main()
