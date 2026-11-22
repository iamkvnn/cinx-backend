"""Readability and timing metrics for rendered subtitles, and the warning codes an instructor reviews.

Pure on purpose: it imports no settings, so ``evals.subtitle_qc`` can use it on a downloaded file
without a .env. The worker passes its configured limits in.
"""

import statistics
from dataclasses import dataclass
from typing import Protocol, Sequence


SCHEMA_VERSION = 1
# A metric becomes a review warning past these shares or values.
CPS_WARN_SHARE = 0.1
SHORT_CUE_WARN_SHARE = 0.1
LOW_CONFIDENCE = 0.5
LOW_CONFIDENCE_WARN_SHARE = 0.15
LANGUAGE_WARN_PROBABILITY = 0.8


class TimedText(Protocol):
    start: float
    end: float
    text: str


@dataclass
class Cue:
    start: float
    end: float
    text: str


@dataclass(frozen=True)
class Thresholds:
    max_line_chars: int
    max_lines: int
    max_cps: float
    min_seconds: float


def quality_report(
    cues: Sequence[TimedText], max_line_chars: int, max_lines: int, max_cps: float, min_seconds: float
) -> dict:
    if not cues:
        return {"cues": 0}
    durations = [max(0.0, cue.end - cue.start) for cue in cues]
    visible = [len(cue.text.replace("\n", "")) for cue in cues]
    cps = [chars / duration if duration > 0 else float("inf") for chars, duration in zip(visible, durations)]
    line_lengths = [len(line) for cue in cues for line in cue.text.splitlines()]
    finite_cps = [value for value in cps if value != float("inf")]
    overlaps = sum(1 for previous, cue in zip(cues, cues[1:]) if cue.start < previous.end)
    backwards = sum(1 for previous, cue in zip(cues, cues[1:]) if cue.start < previous.start)
    # A cue far shorter than a line reads as a fragment of its neighbour.
    orphans = sum(1 for chars in visible if chars < max_line_chars / 3)
    return {
        "cues": len(cues),
        "totalSeconds": round(cues[-1].end - cues[0].start, 2),
        "cps": {
            "p50": _round(statistics.median(finite_cps)) if finite_cps else None,
            "p90": _round(_percentile(finite_cps, 0.9)) if finite_cps else None,
            "max": _round(max(finite_cps)) if finite_cps else None,
            "overLimitShare": _share(sum(1 for value in cps if value > max_cps), len(cues)),
        },
        "lineChars": {
            "max": max(line_lengths, default=0),
            "overLimitShare": _share(sum(1 for length in line_lengths if length > max_line_chars), len(line_lengths)),
        },
        "cuesOverLineCount": sum(1 for cue in cues if len(cue.text.splitlines()) > max_lines),
        "durationSeconds": {
            "min": _round(min(durations)),
            "median": _round(statistics.median(durations)),
            "max": _round(max(durations)),
            "underMinimumShare": _share(sum(1 for duration in durations if duration < min_seconds), len(cues)),
        },
        "overlaps": overlaps,
        "startsBackwards": backwards,
        "orphanCues": orphans,
    }


def doubtful_word_share(items: list[dict]) -> float | None:
    """Share of recognised words below LOW_CONFIDENCE; None when the ASR gave no word scores."""
    scored = [item["confidence"] for item in items if item.get("confidence") is not None]
    if not scored:
        return None
    return _share(sum(1 for confidence in scored if confidence < LOW_CONFIDENCE), len(scored))


def review_summary(
    cues: Sequence[TimedText],
    thresholds: Thresholds,
    doubtful_share: float | None = None,
    language_probability: float | None = None,
) -> dict:
    """The report sent with a completed job; it holds summary numbers only, never subtitle text."""
    metrics = quality_report(
        cues, thresholds.max_line_chars, thresholds.max_lines, thresholds.max_cps, thresholds.min_seconds
    )
    language = _round(language_probability) if language_probability is not None else None
    return {
        "schemaVersion": SCHEMA_VERSION,
        "thresholds": {
            "maxLineChars": thresholds.max_line_chars,
            "maxLines": thresholds.max_lines,
            "maxCps": thresholds.max_cps,
            "minSeconds": thresholds.min_seconds,
        },
        "metrics": metrics,
        "doubtfulWordShare": doubtful_share,
        "languageProbability": language,
        "warnings": warning_codes(metrics, doubtful_share, language),
    }


def warning_codes(metrics: dict, doubtful_share: float | None, language_probability: float | None) -> list[str]:
    if not metrics.get("cues"):
        return []
    codes = []
    if metrics["cps"]["overLimitShare"] > CPS_WARN_SHARE:
        codes.append("CPS_HIGH")
    if metrics["lineChars"]["overLimitShare"] > 0 or metrics["cuesOverLineCount"] > 0:
        codes.append("LINE_TOO_LONG")
    if metrics["durationSeconds"]["underMinimumShare"] > SHORT_CUE_WARN_SHARE:
        codes.append("CUE_TOO_SHORT")
    if metrics["overlaps"] > 0:
        codes.append("OVERLAP")
    if doubtful_share is not None and doubtful_share > LOW_CONFIDENCE_WARN_SHARE:
        codes.append("LOW_CONFIDENCE")
    if language_probability is not None and language_probability < LANGUAGE_WARN_PROBABILITY:
        codes.append("LANGUAGE_UNCERTAIN")
    return codes


def _percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(round(fraction * (len(ordered) - 1))))]


def _share(count: int, total: int) -> float:
    return round(count / total, 4) if total else 0.0


def _round(value: float) -> float:
    return round(float(value), 3)
