import re

from app.models import ASRSegment, SentenceItem


SENTENCE_END_RE = re.compile(r"(?<=[.!?。！？])\s+")


def split_text_by_rules(text: str) -> list[str]:
    """Sentences only; long sentences are split into cues later, at punctuation and pauses."""
    text = re.sub(r"\s+", " ", text or "").strip()
    if not text:
        return []
    return [part.strip() for part in SENTENCE_END_RE.split(text) if part.strip()]


def split_segments_to_sentences(segments: list[ASRSegment]) -> list[SentenceItem]:
    sentences: list[SentenceItem] = []
    next_id = 0
    for segment in segments:
        parts = split_text_by_rules(segment.text)
        if not parts:
            continue

        total_chars = sum(max(1, len(part)) for part in parts)
        cursor = segment.start
        duration = max(0.01, segment.end - segment.start)

        for part in parts:
            part_duration = duration * max(1, len(part)) / total_chars
            sentences.append(
                SentenceItem(
                    id=next_id,
                    source_segment_id=segment.id,
                    start=cursor,
                    end=cursor + part_duration,
                    text=part,
                )
            )
            next_id += 1
            cursor += part_duration
    return sentences
