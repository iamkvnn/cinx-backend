import os
import unittest


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")
os.environ.setdefault("AWS_S3_CDN_URL", "https://cdn.test")
os.environ.setdefault("AWS_REGION", "test-region")
os.environ.setdefault("RABBITMQ_USER", "guest")
os.environ.setdefault("RABBITMQ_PASSWORD", "guest")
os.environ.setdefault("DIGITALOCEAN_LLM_MODEL", "test-model")

from app.models import SentenceItem
from app.services.processor import translate_sentence_items, word_confidence_key


class FakeLlm:
    def translate_batch(self, items, source_language, target_language, analysis):
        return [{"id": item["id"], "text": f"{target_language}:{item['text']}"} for item in items]


class ProcessorTest(unittest.TestCase):
    def test_word_confidence_key(self):
        self.assertEqual("courses/subtitles/ai/l/default.words.json", word_confidence_key("courses/subtitles/ai/l/default.vtt"))

    def test_translate_sentence_items_preserves_timestamps(self):
        source = [SentenceItem(id=1, source_segment_id=1, start=1.0, end=2.0, text="hello")]
        result = translate_sentence_items(source, FakeLlm(), "en", "vi", {})
        self.assertEqual("vi:hello", result[0].text)
        self.assertEqual(1.0, result[0].start)
        self.assertEqual(2.0, result[0].end)


if __name__ == "__main__":
    unittest.main()
