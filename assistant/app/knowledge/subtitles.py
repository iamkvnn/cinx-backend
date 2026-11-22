"""WebVTT and SRT cues, and the slice of them around a playback position."""

import re
from dataclasses import dataclass


TIMESTAMP = re.compile(r"(?:(\d+):)?(\d{1,2}):(\d{2})[.,](\d{3})")
CUE_TIMING = re.compile(rf"({TIMESTAMP.pattern})\s*-->\s*({TIMESTAMP.pattern})")
TAG = re.compile(r"<[^>]+>")


@dataclass(frozen=True)
class Cue:
    start_ms: int
    end_ms: int
    text: str


def _ms(match: re.Match, offset: int) -> int:
    hours, minutes, seconds, millis = (match.group(offset + i) for i in range(1, 5))
    return ((int(hours or 0) * 60 + int(minutes)) * 60 + int(seconds)) * 1000 + int(millis)


def parse(content: str) -> list[Cue]:
    """Both formats are blocks of an optional id, a timing line and text lines, separated by blank lines."""
    cues = []
    for block in re.split(r"\r?\n\s*\r?\n", content or ""):
        lines = [line.strip() for line in block.strip().splitlines()]
        for index, line in enumerate(lines):
            timing = CUE_TIMING.search(line)
            if timing is None:
                continue
            text = " ".join(TAG.sub("", text_line) for text_line in lines[index + 1:] if text_line)
            if text:
                cues.append(Cue(_ms(timing, 1), _ms(timing, 6), text))
            break
    return cues


def clock(ms: int) -> str:
    seconds = ms // 1000
    hours, rest = divmod(seconds, 3600)
    minutes, seconds = divmod(rest, 60)
    return f"{hours}:{minutes:02d}:{seconds:02d}" if hours else f"{minutes:02d}:{seconds:02d}"


def window(cues: list[Cue], around_ms: int | None, before_ms: int, after_ms: int, max_chars: int) -> tuple[list[Cue], bool]:
    """Cues near the position (or from the start), capped in characters. True when cues were left out."""
    if around_ms is not None:
        start, end = max(0, around_ms - before_ms), around_ms + after_ms
        selected = [cue for cue in cues if cue.end_ms >= start and cue.start_ms <= end]
    else:
        selected = list(cues)
    kept, used = [], 0
    for cue in selected:
        used += len(cue.text) + 8
        if used > max_chars:
            break
        kept.append(cue)
    return kept, len(kept) < len(cues)
