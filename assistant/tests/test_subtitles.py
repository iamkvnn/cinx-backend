import unittest


VTT = """WEBVTT

1
00:00:01.000 --> 00:00:04.500
Xin chào, hôm nay ta học <b>vòng lặp</b>.

2
00:02:10.000 --> 00:02:14.000
Vòng for lặp qua từng phần tử.

00:05:00.000 --> 00:05:03.000
Hết bài.
"""

SRT = """1
00:00:01,000 --> 00:00:02,000
Hello

2
01:00:00,250 --> 01:00:01,000
An hour in
"""


class SubtitleParseTest(unittest.TestCase):
    def test_vtt_cues_with_or_without_ids_and_tags_removed(self):
        from app.knowledge.subtitles import parse

        cues = parse(VTT)
        self.assertEqual([(c.start_ms, c.text) for c in cues], [
            (1_000, "Xin chào, hôm nay ta học vòng lặp."),
            (130_000, "Vòng for lặp qua từng phần tử."),
            (300_000, "Hết bài."),
        ])

    def test_srt_comma_millis_and_hours(self):
        from app.knowledge.subtitles import parse

        cues = parse(SRT)
        # 1 h 0 min 0.25 s = 3_600_250 ms
        self.assertEqual([c.start_ms for c in cues], [1_000, 3_600_250])

    def test_clock_formats(self):
        from app.knowledge.subtitles import clock

        self.assertEqual(clock(130_000), "02:10")
        self.assertEqual(clock(3_600_250), "1:00:00")

    def test_window_keeps_cues_near_the_position(self):
        from app.knowledge.subtitles import parse, window

        # Paused at 2:12 with 60 s before and after: only the 2:10 cue overlaps 1:12 to 3:12.
        cues, truncated = window(parse(VTT), 132_000, 60_000, 60_000, 10_000)
        self.assertEqual([c.start_ms for c in cues], [130_000])
        self.assertTrue(truncated)

    def test_window_caps_characters(self):
        from app.knowledge.subtitles import parse, window

        cues, truncated = window(parse(VTT), None, 0, 0, 50)
        self.assertEqual(len(cues), 1)
        self.assertTrue(truncated)


if __name__ == "__main__":
    unittest.main()
