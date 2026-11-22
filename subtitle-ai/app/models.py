from dataclasses import dataclass, asdict
from typing import Any


@dataclass
class WordTimestamp:
    word: str
    start: float
    end: float
    probability: float | None = None


@dataclass
class ASRSegment:
    id: int
    start: float
    end: float
    text: str
    words: list[WordTimestamp]


@dataclass
class SentenceItem:
    id: int
    source_segment_id: int
    start: float
    end: float
    text: str


@dataclass
class SubtitleItem:
    index: int
    start: float
    end: float
    text: str


def asr_segments_from_jsonable(values: list[dict]) -> list[ASRSegment]:
    return [
        ASRSegment(
            id=int(value["id"]),
            start=float(value["start"]),
            end=float(value["end"]),
            text=str(value["text"]),
            words=[
                WordTimestamp(
                    word=str(word["word"]),
                    start=float(word["start"]),
                    end=float(word["end"]),
                    probability=word.get("probability"),
                )
                for word in value.get("words") or []
            ],
        )
        for value in values
    ]


def to_jsonable(value: Any) -> Any:
    if hasattr(value, "__dataclass_fields__"):
        return asdict(value)
    if isinstance(value, list):
        return [to_jsonable(item) for item in value]
    if isinstance(value, dict):
        return {key: to_jsonable(item) for key, item in value.items()}
    return value
