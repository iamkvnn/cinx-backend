"""Readability and timing report for a WebVTT file, with optional WER/CER against a reference.

Run from ``subtitle-ai``:
    python -m evals.subtitle_qc output.vtt [--reference transcript.txt] [--json]

It needs neither the service settings nor a .env, so it can check a file
downloaded from storage: the metrics come from ``app.services.quality``, which
the worker also sends with each job.  The limits default to the service's
defaults and can be overridden to match its configuration.
"""

import argparse
import json
import re
import sys
import unicodedata
from pathlib import Path

from app.services.quality import Cue, quality_report


TIMESTAMP_RE = re.compile(r"(?:(\d+):)?(\d{2}):(\d{2})\.(\d{3})\s+-->\s+(?:(\d+):)?(\d{2}):(\d{2})\.(\d{3})")
# Scripts written without spaces are compared by character.
NO_SPACE_SCRIPT_RE = re.compile(r"[぀-ヿ㐀-鿿฀-๿຀-໿ក-៿က-႟]")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("vtt", type=Path)
    parser.add_argument("--reference", type=Path, help="plain-text transcript of the same audio")
    parser.add_argument("--max-line-chars", type=int, default=52)
    parser.add_argument("--max-lines", type=int, default=1)
    parser.add_argument("--max-cps", type=float, default=20.0)
    parser.add_argument("--min-seconds", type=float, default=1.0)
    parser.add_argument("--json", action="store_true", help="print the report as JSON")
    args = parser.parse_args(argv)

    cues = parse_vtt(args.vtt.read_text(encoding="utf-8-sig"))
    report = quality_report(cues, args.max_line_chars, args.max_lines, args.max_cps, args.min_seconds)
    if args.reference:
        hypothesis = " ".join(cue.text for cue in cues)
        report["accuracy"] = error_rates(args.reference.read_text(encoding="utf-8"), hypothesis)

    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        _print_report(report)
    return 0


def parse_vtt(content: str) -> list[Cue]:
    cues: list[Cue] = []
    for block in re.split(r"\n\s*\n", content.replace("\r\n", "\n").replace("\r", "\n").strip()):
        lines = [line.strip() for line in block.splitlines() if line.strip()]
        timing = next((index for index, line in enumerate(lines) if "-->" in line), None)
        if timing is None:
            continue
        match = TIMESTAMP_RE.search(lines[timing])
        text = "\n".join(lines[timing + 1 :])
        if match and text:
            values = match.groups()
            cues.append(Cue(_seconds(values[:4]), _seconds(values[4:]), text))
    return cues


def error_rates(reference: str, hypothesis: str) -> dict:
    """WER on words and CER on characters; for scripts without spaces only CER is meaningful."""
    reference_text = _normalise(reference)
    hypothesis_text = _normalise(hypothesis)
    reference_words = reference_text.split()
    reference_chars = list(reference_text.replace(" ", ""))
    return {
        "wer": _round(_edit_distance(reference_words, hypothesis_text.split()) / max(1, len(reference_words))),
        "cer": _round(_edit_distance(reference_chars, list(hypothesis_text.replace(" ", ""))) / max(1, len(reference_chars))),
        "noSpaceScript": bool(NO_SPACE_SCRIPT_RE.search(reference_text)),
        "referenceWords": len(reference_words),
    }


def _edit_distance(reference: list[str], hypothesis: list[str]) -> int:
    # O(n * m) time, O(m) memory: meant for clips, not hour-long transcripts.
    previous = list(range(len(hypothesis) + 1))
    for row, reference_item in enumerate(reference, start=1):
        current = [row] + [0] * len(hypothesis)
        for column, hypothesis_item in enumerate(hypothesis, start=1):
            current[column] = min(
                previous[column] + 1,
                current[column - 1] + 1,
                previous[column - 1] + (reference_item != hypothesis_item),
            )
        previous = current
    return previous[-1]


def _normalise(text: str) -> str:
    text = unicodedata.normalize("NFC", text.lower())
    text = "".join(character if character.isalnum() or character.isspace() else " " for character in text)
    return " ".join(text.split())


def _seconds(parts) -> float:
    hours, minutes, seconds, millis = parts
    return int(hours or 0) * 3600 + int(minutes) * 60 + int(seconds) + int(millis) / 1000


def _round(value: float) -> float:
    return round(float(value), 3)


def _print_report(report: dict) -> None:
    if not report.get("cues"):
        print("No cues found.")
        return
    print(f"Cues: {report['cues']} over {report['totalSeconds']} s")
    cps = report["cps"]
    print(f"CPS: p50 {cps['p50']}  p90 {cps['p90']}  max {cps['max']}  over limit {cps['overLimitShare']:.1%}")
    lines = report["lineChars"]
    print(f"Line length: max {lines['max']}  over limit {lines['overLimitShare']:.1%}")
    print(f"Cues over the line count: {report['cuesOverLineCount']}")
    durations = report["durationSeconds"]
    print(
        f"Duration: min {durations['min']}  median {durations['median']}  max {durations['max']}  "
        f"under minimum {durations['underMinimumShare']:.1%}"
    )
    print(f"Overlaps: {report['overlaps']}  starts moving backwards: {report['startsBackwards']}  orphan cues: {report['orphanCues']}")
    accuracy = report.get("accuracy")
    if accuracy:
        print(
            f"WER {accuracy['wer']:.1%}  CER {accuracy['cer']:.1%}  "
            f"({accuracy['referenceWords']} reference words{', no-space script: use CER' if accuracy['noSpaceScript'] else ''})"
        )


if __name__ == "__main__":
    sys.exit(main())
