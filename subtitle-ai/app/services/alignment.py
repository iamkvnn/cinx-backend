import re

from app.models import ASRSegment, SentenceItem, WordTimestamp


TOKEN_RE = re.compile(r"\w+", re.UNICODE)


def align_sentence_items_to_words(
    items: list[SentenceItem],
    segments: list[ASRSegment],
) -> list[SentenceItem]:
    words_by_segment = _words_by_segment(segments)
    cursors: dict[int, int] = {}
    aligned: list[SentenceItem] = []

    for item in items:
        words = words_by_segment.get(item.source_segment_id, [])
        cursor = cursors.get(item.source_segment_id, 0)
        span = find_word_span(item.text, words, cursor)
        if span is None:
            aligned.append(item)
            continue

        start_index, end_index = span
        cursors[item.source_segment_id] = end_index + 1
        aligned.append(
            SentenceItem(
                id=item.id,
                source_segment_id=item.source_segment_id,
                start=words[start_index].start,
                end=words[end_index].end,
                text=item.text,
            )
        )
    return aligned


def distribute_chunks_with_word_alignment(
    start: float,
    end: float,
    chunks: list[str],
    words: list[WordTimestamp],
) -> list[dict]:
    cleaned = [chunk for chunk in chunks if chunk and chunk.strip()]
    if not cleaned:
        return []

    scoped_words = [
        word
        for word in words
        if word.word.strip() and word.end >= start - 0.05 and word.start <= end + 0.05
    ]
    if not scoped_words:
        return []

    cursor = 0
    result: list[dict] = []
    for chunk in cleaned:
        span = find_word_span(chunk, scoped_words, cursor)
        if span is None:
            return []

        start_index, end_index = span
        result.append(
            {
                "start": max(start, scoped_words[start_index].start),
                "end": min(end, scoped_words[end_index].end),
                "text": chunk,
            }
        )
        cursor = end_index + 1
    return result


def find_word_span(
    text: str,
    words: list[WordTimestamp],
    cursor: int = 0,
) -> tuple[int, int] | None:
    text_tokens = _tokens(text)
    if not text_tokens or not words:
        return None

    word_tokens = [_tokens(word.word) for word in words]
    flattened = [tokens[0] if tokens else "" for tokens in word_tokens]
    matched: list[int] = []
    position = max(0, cursor)

    for token in text_tokens:
        found = _find_token(flattened, token, position)
        if found is None:
            continue
        matched.append(found)
        position = found + 1

    if not matched:
        return None

    match_ratio = len(matched) / len(text_tokens)
    if match_ratio < 0.6:
        return None

    return matched[0], matched[-1]


def _words_by_segment(segments: list[ASRSegment]) -> dict[int, list[WordTimestamp]]:
    return {
        segment.id: sorted(
            [word for word in segment.words if word.word.strip()],
            key=lambda word: (word.start, word.end),
        )
        for segment in segments
    }


def _tokens(text: str) -> list[str]:
    return [match.group(0).lower() for match in TOKEN_RE.finditer(text or "")]


def _find_token(tokens: list[str], wanted: str, cursor: int) -> int | None:
    for index in range(cursor, len(tokens)):
        if tokens[index] == wanted:
            return index
    return None
