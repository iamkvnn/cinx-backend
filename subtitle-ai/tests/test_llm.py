import os
import unittest
from unittest.mock import patch


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")
os.environ.setdefault("AWS_S3_CDN_URL", "https://cdn.test")
os.environ.setdefault("AWS_REGION", "test-region")
os.environ.setdefault("RABBITMQ_USER", "guest")
os.environ.setdefault("RABBITMQ_PASSWORD", "guest")
os.environ.setdefault("DIGITALOCEAN_LLM_MODEL", "test-model")

from app.services.llm import DigitalOceanLLM, OpenAIProviderError, parse_json_object


class LlmTest(unittest.TestCase):
    def test_parse_json_from_wrapped_content(self):
        payload = parse_json_object("Here is JSON:\n```json\n{\"chunks\": [\"a\", \"b\"]}\n```")
        self.assertEqual(["a", "b"], payload["chunks"])

    def test_invalid_json_returns_empty_dict(self):
        self.assertEqual({}, parse_json_object("not json"))

    def test_split_subtitle_timeout_retries_then_raises(self):
        llm = DigitalOceanLLM()
        llm.api_key = "test-key"
        with (
            patch.object(llm, "_chat_json", side_effect=OpenAIProviderError("timeout")) as chat,
            patch("app.services.llm.settings.DIGITALOCEAN_LLM_MAX_RETRIES", 3),
            patch("app.services.llm.settings.DIGITALOCEAN_LLM_RETRY_DELAY_SECONDS", 0),
        ):
            with self.assertRaises(OpenAIProviderError):
                llm.split_subtitle_batch([{"id": 1, "text": "hello world"}], "vi")

        self.assertEqual(4, chat.call_count)


if __name__ == "__main__":
    unittest.main()
