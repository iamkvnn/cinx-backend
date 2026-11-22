import logging
import re
from pathlib import Path

from app.core.config import settings
from app.models import ASRSegment, SentenceItem, SubtitleItem, WordTimestamp
from app.services.alignment import distribute_chunks_with_word_alignment
from app.services.artifacts import write_json_artifact
from app.services.cue_splitter import (
    CLOSING_MARKS,
    SENTENCE_END,
    cue_char_limit,
    is_no_space_language,
    render_chunk,
    split_ranges,
    split_text,
    tokenize,
)


VTT_TIMESTAMP = re.compile(
    r"(?P<start>\d{2}:\d{2}:\d{2}\.\d{3})\s+-->\s+(?P<end>\d{2}:\d{2}:\d{2}\.\d{3})"
)
MIN_CUE_SECONDS = 0.25
WORD_SCOPE_TOLERANCE_SECONDS = 0.01
# Source cues merged into one sentence for translation.
GROUP_MAX_CUES = 4
GROUP_MAX_CHARS = 250
GROUP_MAX_GAP_SECONDS = 1.0


def parse_webvtt(content: str) -> list[SubtitleItem]:
    normalized = strip_bom(content).replace("\r\n", "\n").replace("\r", "\n")
    blocks = re.split(r"\n\s*\n", normalized.strip())
    items: list[SubtitleItem] = []
    for block in blocks:
        lines = [line.strip() for line in block.splitlines() if line.strip()]
        if not lines or lines[0].upper().startswith("WEBVTT"):
            continue
        timestamp_index = next((idx for idx, line in enumerate(lines) if "-->" in line), None)
        if timestamp_index is None:
            continue
        match = VTT_TIMESTAMP.search(lines[timestamp_index])
        if not match:
            continue
        text = "\n".join(lines[timestamp_index + 1 :]).strip()
        if not text:
            continue
        items.append(
            SubtitleItem(
                index=len(items) + 1,
                start=parse_vtt_timestamp(match.group("start")),
                end=parse_vtt_timestamp(match.group("end")),
                text=text,
            )
        )
    return items


def build_final_subtitles(
    items: list[SentenceItem],
    llm,
    target_language: str,
    artifact_dir: Path | None = None,
    source_segments: list[ASRSegment] | None = None,
    cue_times: dict[int, list[tuple[float, float]]] | None = None,
) -> list[SubtitleItem]:
    """Cues for each item: ASR word times when generating, source cue times when translating."""
    output: list[SubtitleItem] = []
    candidates = [item for item in items if item.text.strip()]
    llm_chunks = split_translated_items_for_subtitles(candidates, llm, target_language, artifact_dir)
    words_by_segment = {
        segment.id: sorted(
            [word for word in segment.words if word.word.strip()],
            key=lambda word: (word.start, word.end),
        )
        for segment in (source_segments or [])
    }

    split_map: dict[int, list[str]] = {}
    for item in candidates:
        segment_words = words_by_segment.get(item.source_segment_id, [])
        if item.id in llm_chunks:
            chunks = llm_chunks[item.id]
            cues = distribute_chunks_with_word_alignment(
                item.start, item.end, chunks, segment_words
            ) or distribute_time(item.start, item.end, chunks)
        else:
            cues = split_item_into_cues(item, segment_words, target_language, (cue_times or {}).get(item.id))
        split_map[item.id] = [cue["text"] for cue in cues]
        for cue in cues:
            output.append(
                SubtitleItem(
                    index=len(output) + 1,
                    start=cue["start"],
                    end=cue["end"],
                    text=cue["text"],
                )
            )
    write_json_artifact(artifact_dir, "08_final_subtitle_split_map.json", split_map)
    normalize_timing(output)
    return output


def split_item_into_cues(
    item: SentenceItem,
    segment_words: list[WordTimestamp],
    language: str,
    times: list[tuple[float, float]] | None = None,
) -> list[dict]:
    tokens = tokenize(item.text, language)
    words = matching_words(tokens, segment_words, item)
    pauses = [words[index + 1].start - words[index].end for index in range(len(words) - 1)] if words else None
    ranges = split_ranges(tokens, language, pauses, min_chunks=len(times) if times else 1)
    texts = [render_chunk(tokens[lo:hi], language) for lo, hi in ranges]
    if words:
        return [
            {"start": words[lo].start, "end": words[hi - 1].end, "text": text}
            for (lo, hi), text in zip(ranges, texts)
        ]
    if times and len(times) == len(texts):
        return [{"start": start, "end": end, "text": text} for (start, end), text in zip(times, texts)]
    return distribute_chunks_with_word_alignment(
        item.start, item.end, texts, segment_words
    ) or distribute_time(item.start, item.end, texts)


def matching_words(tokens, segment_words: list[WordTimestamp], item: SentenceItem) -> list[WordTimestamp] | None:
    """The item's ASR words when they match its tokens one to one, which gives exact cue times."""
    if not tokens or not segment_words:
        return None
    scoped = [
        word
        for word in segment_words
        if word.start >= item.start - WORD_SCOPE_TOLERANCE_SECONDS
        and word.end <= item.end + WORD_SCOPE_TOLERANCE_SECONDS
    ]
    if len(scoped) != len(tokens):
        return None
    if any(_plain(token.text) != _plain(word.word) for token, word in zip(tokens, scoped)):
        return None
    return scoped


def normalize_timing(subtitles: list[SubtitleItem]) -> None:
    """Cues never move backwards or overlap; short cues borrow the following gap to be readable."""
    previous: SubtitleItem | None = None
    for sub in subtitles:
        if previous is not None:
            sub.start = max(sub.start, previous.start)
            if previous.end > sub.start:
                previous.end = max(sub.start, previous.start + MIN_CUE_SECONDS)
                sub.start = max(sub.start, previous.end)
        sub.end = max(sub.end, sub.start + MIN_CUE_SECONDS)
        previous = sub

    for index, sub in enumerate(subtitles):
        visible_chars = len(sub.text.replace("\n", ""))
        wanted = sub.start + max(settings.MIN_SUBTITLE_SECONDS, visible_chars / max(1, settings.MAX_SUBTITLE_CPS))
        if wanted <= sub.end:
            continue
        if index + 1 < len(subtitles):
            sub.end = max(sub.end, min(wanted, subtitles[index + 1].start))
        else:
            sub.end = wanted


def group_sentence_items(
    items: list[SentenceItem],
    language: str,
) -> tuple[list[SentenceItem], dict[int, list[tuple[float, float]]]]:
    """Merge consecutive cues into sentences; the second value keeps each group's cue times."""
    joiner = "" if is_no_space_language(language) else " "
    groups: list[list[SentenceItem]] = []
    current: list[SentenceItem] = []
    for item in items:
        if current:
            last = current[-1]
            merged_chars = len(joiner.join(member.text for member in current)) + len(joiner) + len(item.text)
            if (
                ends_sentence(last.text)
                or item.start - last.end > GROUP_MAX_GAP_SECONDS
                or len(current) >= GROUP_MAX_CUES
                or merged_chars > GROUP_MAX_CHARS
            ):
                groups.append(current)
                current = []
        current.append(item)
    if current:
        groups.append(current)

    grouped: list[SentenceItem] = []
    cue_times: dict[int, list[tuple[float, float]]] = {}
    for index, group in enumerate(groups):
        grouped.append(
            SentenceItem(
                id=index,
                source_segment_id=index,
                start=group[0].start,
                end=group[-1].end,
                text=joiner.join(member.text for member in group),
            )
        )
        cue_times[index] = [(member.start, member.end) for member in group]
    return grouped, cue_times


def ends_sentence(text: str) -> bool:
    last = text.rstrip().rstrip(CLOSING_MARKS)[-1:]
    return bool(last) and last in SENTENCE_END


def sentence_items_from_cues(cues: list[SubtitleItem]) -> list[SentenceItem]:
    return [
        SentenceItem(
            id=index,
            source_segment_id=index,
            start=cue.start,
            end=cue.end,
            text=" ".join(cue.text.split()),
        )
        for index, cue in enumerate(cues)
    ]


def write_webvtt(subtitles: list[SubtitleItem]) -> str:
    lines = ["WEBVTT", ""]
    for sub in subtitles:
        lines.append(f"{format_vtt_timestamp(sub.start)} --> {format_vtt_timestamp(sub.end)}")
        lines.extend(sub.text.splitlines())
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def validate_subtitles(subtitles: list[SubtitleItem]) -> None:
    if not subtitles:
        raise ValueError("No subtitle cues generated")
    previous_start = -1.0
    for index, sub in enumerate(subtitles, start=1):
        if sub.index != index:
            raise ValueError("Subtitle indexes must be sequential")
        if sub.start < previous_start:
            raise ValueError("Subtitle start time moved backwards")
        if sub.end <= sub.start:
            raise ValueError("Subtitle end must be greater than start")
        lines = sub.text.splitlines()
        if not lines or len(lines) > settings.MAX_SUBTITLE_LINES:
            raise ValueError("Subtitle text violates line count constraints")
        for line in lines:
            if len(line) > settings.MAX_SUBTITLE_LINE_CHARS + 12:
                raise ValueError("Subtitle line is too long")
        previous_start = sub.start


def distribute_time(start: float, end: float, chunks: list[str]) -> list[dict]:
    cleaned = [chunk for chunk in chunks if chunk and chunk.strip()]
    if not cleaned:
        return []
    if len(cleaned) == 1:
        return [{"start": start, "end": end, "text": cleaned[0]}]

    duration = max(0.01, end - start)
    weights = [max(1, len(chunk.replace("\n", " "))) for chunk in cleaned]
    total = sum(weights)
    cursor = start
    result: list[dict] = []
    for index, chunk in enumerate(cleaned):
        chunk_end = end if index == len(cleaned) - 1 else cursor + duration * weights[index] / total
        result.append({"start": cursor, "end": chunk_end, "text": chunk})
        cursor = chunk_end
    return result


def normalize_text(text: str) -> str:
    return re.sub(r"\s+", " ", text or "").strip()


def sanitize_llm_chunks(original: str, chunks: list[str], language: str | None = None) -> list[str]:
    if not chunks:
        return split_text(original, language)

    result: list[str] = []
    for chunk in chunks:
        if not isinstance(chunk, str):
            continue
        lines = [normalize_text(line) for line in chunk.split("\n")]
        chunk = "\n".join([line for line in lines if line])
        if is_valid_chunk(chunk):
            result.append(chunk)
        else:
            result.extend(split_text(chunk, language))
    return result or split_text(original, language)


def is_valid_chunk(chunk: str) -> bool:
    if not chunk or not chunk.strip():
        return False
    lines = chunk.splitlines()
    if len(lines) > settings.MAX_SUBTITLE_LINES:
        return False
    return all(len(line.strip()) <= settings.MAX_SUBTITLE_LINE_CHARS + 12 for line in lines)


def needs_final_split(item: SentenceItem, language: str | None = None) -> bool:
    text = normalize_text(item.text)
    duration = max(0.01, item.end - item.start)
    if len(text) > cue_char_limit():
        return True
    if len(text) / duration > settings.MAX_SUBTITLE_CPS and len(text) > settings.MAX_SUBTITLE_LINE_CHARS:
        return True
    return len(split_text(text, language)) > 1


def split_translated_items_for_subtitles(
    items: list[SentenceItem],
    llm,
    target_language: str,
    artifact_dir: Path | None = None,
) -> dict[int, list[str]]:
    """LLM chunks for the items that need them when the LLM split is enabled; the rest are split locally."""
    result: dict[int, list[str]] = {}
    if not settings.USE_LLM_FOR_FINAL_SUBTITLE_SPLIT:
        return result
    need_llm = [item for item in items if needs_final_split(item, target_language)]

    for batch_index, batch in enumerate(chunk_list(need_llm, settings.FINAL_SPLIT_BATCH_SIZE), start=1):
        llm_input = [
            {"id": item.id, "text": item.text, "duration_seconds": item.end - item.start}
            for item in batch
        ]
        write_json_artifact(artifact_dir, f"07_llm_final_split_batch_{batch_index}_input.json", llm_input)
        try:
            llm_output = llm.split_subtitle_batch(llm_input, target_language)
            write_json_artifact(artifact_dir, f"07_llm_final_split_batch_{batch_index}_output.json", llm_output)
            output_map = normalize_split_output(llm_output)
            for item in batch:
                result[item.id] = sanitize_llm_chunks(item.text, output_map.get(item.id, []), target_language)
        except Exception as exc:
            # Items left out of the result are split locally.
            logging.getLogger(__name__).warning("LLM final split failed, using rule-based split: %s", exc)
            write_json_artifact(
                artifact_dir,
                f"07_llm_final_split_batch_{batch_index}_fallback.json",
                {"error": repr(exc), "input": llm_input},
            )
    return result


def normalize_split_output(value) -> dict[int, list[str]]:
    if isinstance(value, dict):
        return {int(key): chunks for key, chunks in value.items() if str(key).isdigit() and isinstance(chunks, list)}
    result: dict[int, list[str]] = {}
    if not isinstance(value, list):
        return result
    for item in value:
        if not isinstance(item, dict) or "id" not in item:
            continue
        chunks = item.get("chunks", [])
        if isinstance(chunks, list):
            result[int(item["id"])] = chunks
    return result


def format_vtt_timestamp(seconds: float) -> str:
    seconds = max(0.0, float(seconds))
    millis = int(round((seconds - int(seconds)) * 1000))
    total_seconds = int(seconds)
    if millis >= 1000:
        total_seconds += 1
        millis -= 1000
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    secs = total_seconds % 60
    return f"{hours:02}:{minutes:02}:{secs:02}.{millis:03}"


def parse_vtt_timestamp(value: str) -> float:
    hours, minutes, rest = value.split(":")
    seconds, millis = rest.split(".")
    return int(hours) * 3600 + int(minutes) * 60 + int(seconds) + int(millis) / 1000


def _plain(text: str) -> str:
    return "".join(character for character in text.lower() if character.isalnum())


def strip_bom(content: str) -> str:
    return content[1:] if content.startswith("\ufeff") else content


def chunk_list(items: list, size: int) -> list[list]:
    return [items[index : index + size] for index in range(0, len(items), size)]
