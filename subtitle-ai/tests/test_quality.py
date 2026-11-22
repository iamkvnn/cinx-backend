import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, Mock, patch


os.environ.setdefault("AWS_S3_BUCKET", "test-bucket")

from app.models import SentenceItem, SubtitleItem
from app.services.processor import SubtitleJobProcessor
from app.services.quality import Cue, Thresholds, doubtful_word_share, quality_report, review_summary
from evals.subtitle_qc import parse_vtt


LIMITS = Thresholds(max_line_chars=52, max_lines=1, max_cps=20, min_seconds=1.0)


class QualityReportTest(unittest.TestCase):
    def test_clean_cues_have_no_warnings(self):
        cues = [Cue(0.0, 2.5, "Xin chào các bạn đến với khoá học"), Cue(2.6, 5.0, "Hôm nay ta học vòng lặp for")]
        summary = review_summary(cues, LIMITS)
        self.assertEqual([], summary["warnings"])
        self.assertEqual(1, summary["schemaVersion"])
        self.assertEqual(52, summary["thresholds"]["maxLineChars"])
        self.assertEqual(2, summary["metrics"]["cues"])

    def test_fast_reading_warns_cps(self):
        # 45 characters in one second is far over 20 per second
        cues = [Cue(0.0, 1.0, "a" * 45), Cue(1.0, 4.0, "Một câu bình thường")]
        summary = review_summary(cues, LIMITS)
        self.assertEqual(0.5, summary["metrics"]["cps"]["overLimitShare"])
        self.assertIn("CPS_HIGH", summary["warnings"])

    def test_long_line_warns(self):
        cues = [Cue(0.0, 6.0, "x" * 60)]
        self.assertIn("LINE_TOO_LONG", review_summary(cues, LIMITS)["warnings"])

    def test_extra_line_warns_as_too_long(self):
        cues = [Cue(0.0, 4.0, "dòng một\ndòng hai")]
        summary = review_summary(cues, LIMITS)
        self.assertEqual(1, summary["metrics"]["cuesOverLineCount"])
        self.assertIn("LINE_TOO_LONG", summary["warnings"])

    def test_overlap_and_short_cues_warn(self):
        cues = [Cue(0.0, 2.0, "Một"), Cue(1.5, 1.9, "Hai"), Cue(2.0, 2.5, "Ba")]
        summary = review_summary(cues, LIMITS)
        self.assertEqual(1, summary["metrics"]["overlaps"])
        self.assertIn("OVERLAP", summary["warnings"])
        self.assertIn("CUE_TOO_SHORT", summary["warnings"])

    def test_vietnamese_counts_composed_letters_once(self):
        report = quality_report([Cue(0.0, 2.0, "Tiếng Việt")], 52, 1, 20, 1.0)
        self.assertEqual(10, report["lineChars"]["max"])

    def test_confidence_and_language_warnings(self):
        items = [{"word": "a", "confidence": 0.2}, {"word": "b", "confidence": 0.9}, {"word": "c", "confidence": None}]
        share = doubtful_word_share(items)
        self.assertEqual(0.5, share)
        summary = review_summary([Cue(0.0, 2.0, "Xin chào")], LIMITS, share, 0.55)
        self.assertIn("LOW_CONFIDENCE", summary["warnings"])
        self.assertIn("LANGUAGE_UNCERTAIN", summary["warnings"])
        self.assertEqual(0.55, summary["languageProbability"])

    def test_no_word_scores_means_no_share(self):
        self.assertIsNone(doubtful_word_share([{"word": "a", "confidence": None}]))

    def test_empty_cues(self):
        self.assertEqual({"cues": 0}, quality_report([], 52, 1, 20, 1.0))
        self.assertEqual([], review_summary([], LIMITS)["warnings"])

    def test_report_holds_no_subtitle_text(self):
        summary = review_summary([Cue(0.0, 2.0, "bí mật")], LIMITS)
        self.assertNotIn("bí mật", str(summary))

    def test_eval_parses_and_shares_the_report(self):
        cues = parse_vtt("WEBVTT\n\n00:00:00.000 --> 00:00:02.000\nXin chào\n\n00:00:02.000 --> 00:00:04.000\nTạm biệt\n")
        self.assertEqual(2, quality_report(cues, 52, 1, 20, 1.0)["cues"])


class CompletedPayloadTest(unittest.IsolatedAsyncioTestCase):
    async def test_translate_payload_carries_quality(self):
        storage = Mock()
        storage.read_text.return_value = "WEBVTT\n\n00:00:00.000 --> 00:00:02.000\nXin chào\n"
        storage.upload_text.return_value = {"fileKey": "courses/subtitles/ai/l/en.vtt", "fileUrl": "https://cdn/en.vtt", "fileSize": 40}
        llm = Mock()
        llm.summarize_and_extract_terms.return_value = {}
        processor = SubtitleJobProcessor(storage=storage, llm=llm)
        translated = [SentenceItem(id=1, source_segment_id=1, start=0.0, end=2.0, text="Hello")]
        rendered = [SubtitleItem(index=1, start=0.0, end=2.0, text="Hello")]
        event = {
            "jobId": "job-1",
            "sourceFileKey": "courses/subtitles/l/vi.vtt",
            "expectedOutputFileKey": "courses/subtitles/ai/l/en.vtt",
            "sourceLanguageCode": "vi",
            "targetLanguageCode": "en",
        }
        with tempfile.TemporaryDirectory() as work_dir, \
                patch("app.services.processor.translate_sentence_items", return_value=translated), \
                patch.object(processor, "_render_subtitles", return_value=("WEBVTT\n", rendered)):
            payload = await processor._translate(event, AsyncMock(), Path(work_dir), 1)
            self.assertTrue((Path(work_dir) / "12_quality.json").exists())

        self.assertEqual(1, payload["quality"]["schemaVersion"])
        self.assertEqual(1, payload["quality"]["metrics"]["cues"])
        self.assertIsNone(payload["quality"]["doubtfulWordShare"])
        self.assertEqual([], payload["quality"]["warnings"])


if __name__ == "__main__":
    unittest.main()
