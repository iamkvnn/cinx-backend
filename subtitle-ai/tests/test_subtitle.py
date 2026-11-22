import os
import unittest


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")
os.environ.setdefault("AWS_S3_CDN_URL", "https://cdn.test")
os.environ.setdefault("AWS_REGION", "test-region")
os.environ.setdefault("RABBITMQ_USER", "guest")
os.environ.setdefault("RABBITMQ_PASSWORD", "guest")
os.environ.setdefault("DIGITALOCEAN_LLM_MODEL", "test-model")

from app.models import ASRSegment, SentenceItem, WordTimestamp
from app.services.alignment import align_sentence_items_to_words
from app.services.subtitle import (
    build_final_subtitles,
    distribute_time,
    format_vtt_timestamp,
    parse_webvtt,
    validate_subtitles,
    write_webvtt,
)


class NoopLlm:
    def split_subtitle_batch(self, items, target_language):
        return {}


class SubtitleTest(unittest.TestCase):
    def test_vtt_parse_and_write(self):
        cues = parse_webvtt(
            "WEBVTT\n\n"
            "00:00:01.000 --> 00:00:02.500\n"
            "Hello world\n\n"
            "00:00:03.000 --> 00:00:04.000\n"
            "Second cue\n"
        )
        self.assertEqual(2, len(cues))
        self.assertEqual("00:00:01.000", format_vtt_timestamp(cues[0].start))
        rendered = write_webvtt(cues)
        self.assertTrue(rendered.startswith("WEBVTT\n\n"))
        self.assertIn("00:00:03.000 --> 00:00:04.000", rendered)

    def test_distribute_time_preserves_bounds(self):
        result = distribute_time(10.0, 14.0, ["short", "a much longer chunk"])
        self.assertEqual(10.0, result[0]["start"])
        self.assertEqual(14.0, result[-1]["end"])
        self.assertGreaterEqual(result[1]["start"], result[0]["end"])

    def test_build_final_subtitles_fallback_split(self):
        item = SentenceItem(
            id=1,
            source_segment_id=1,
            start=0.0,
            end=8.0,
            text=" ".join(["word"] * 40),
        )
        subtitles = build_final_subtitles([item], NoopLlm(), "en")
        validate_subtitles(subtitles)
        self.assertGreater(len(subtitles), 1)

    def test_align_sentence_items_to_word_timestamps(self):
        segment = ASRSegment(
            id=7,
            start=0.0,
            end=5.0,
            text="hello world again",
            words=[
                WordTimestamp("hello", 0.4, 0.8),
                WordTimestamp("world", 1.0, 1.4),
                WordTimestamp("again", 2.0, 2.5),
            ],
        )
        item = SentenceItem(id=1, source_segment_id=7, start=0.0, end=5.0, text="world again")

        result = align_sentence_items_to_words([item], [segment])

        self.assertEqual(1.0, result[0].start)
        self.assertEqual(2.5, result[0].end)

    def test_build_final_subtitles_uses_word_timestamps_for_generate(self):
        segment = ASRSegment(
            id=3,
            start=0.0,
            end=4.0,
            text="hello world again",
            words=[
                WordTimestamp("hello", 0.2, 0.6),
                WordTimestamp("world", 1.0, 1.5),
                WordTimestamp("again", 2.5, 3.0),
            ],
        )
        item = SentenceItem(id=1, source_segment_id=3, start=0.0, end=4.0, text="hello world again")

        subtitles = build_final_subtitles([item], NoopLlm(), "en", source_segments=[segment])

        self.assertEqual(0.2, subtitles[0].start)
        self.assertEqual(3.0, subtitles[0].end)


if __name__ == "__main__":
    unittest.main()
