import math
import re
import unicodedata
from dataclasses import dataclass

from app.core.config import settings


# Scripts written without spaces between words are split by character.
NO_SPACE_LANGUAGES = {"zh", "ja", "th", "lo", "km", "my", "yue", "bo"}
SENTENCE_END = ".!?。！？…"
CLAUSE_END = ",;:，；：、"
CLOSING_MARKS = "\"'”’)]}»」』"
# Break scoring, in units of one line of imbalance.
SENTENCE_BONUS = 0.35
CLAUSE_BONUS = 0.2
PAUSE_BONUS = 0.4
PAUSE_FULL_SECONDS = 0.6
# No chunk shorter than this share of the balanced chunk length.
SHORTEST_SHARE = 1 / 3
SCRIPT_TOKEN_RE = re.compile(r"\s+|[0-9A-Za-zÀ-ɏ]+(?:['’.\-][0-9A-Za-zÀ-ɏ]+)*|\S")


@dataclass
class Token:
    text: str
    space_before: bool


def is_no_space_language(language: str | None) -> bool:
    base = re.split(r"[-_]", (language or "").strip().lower(), maxsplit=1)[0]
    return base in NO_SPACE_LANGUAGES


def cue_char_limit() -> int:
    return settings.MAX_SUBTITLE_LINE_CHARS * max(1, settings.MAX_SUBTITLE_LINES)


def tokenize(text: str, language: str | None) -> list[Token]:
    text = " ".join((text or "").split())
    if not text:
        return []
    if is_no_space_language(language):
        tokens = _script_tokens(text)
    else:
        tokens = [Token(word, index > 0) for index, word in enumerate(text.split(" "))]
    return _break_long_tokens(tokens, settings.MAX_SUBTITLE_LINE_CHARS)


def join_tokens(tokens: list[Token]) -> str:
    return "".join(
        (" " if token.space_before and index else "") + token.text
        for index, token in enumerate(tokens)
    )


def split_ranges(
    tokens: list[Token],
    language: str | None,
    pauses: list[float] | None = None,
    min_chunks: int = 1,
) -> list[tuple[int, int]]:
    """Token ranges for the fewest balanced cues, or `min_chunks` when each would stay readable."""
    if not tokens:
        return []
    max_chars = cue_char_limit()
    splitter = _Splitter(tokens, max_chars, pauses, is_no_space_language(language))
    total = splitter.length(0, len(tokens))
    chunks = max(1, math.ceil(total / max_chars))
    if min_chunks > chunks:
        readable = max(1, int(total // max(1, max_chars * SHORTEST_SHARE)))
        chunks = max(chunks, min(min_chunks, len(tokens), readable))
    return splitter.split(0, len(tokens), chunks, grow=True)


def render_chunk(tokens: list[Token], language: str | None) -> str:
    """One cue's text, with balanced line breaks when a cue may hold several lines."""
    line_chars = settings.MAX_SUBTITLE_LINE_CHARS
    max_lines = max(1, settings.MAX_SUBTITLE_LINES)
    splitter = _Splitter(tokens, line_chars, None, is_no_space_language(language))
    lines = min(max_lines, max(1, math.ceil(splitter.length(0, len(tokens)) / line_chars)))
    if lines <= 1:
        return join_tokens(tokens)
    return "\n".join(join_tokens(tokens[lo:hi]) for lo, hi in splitter.split(0, len(tokens), lines, grow=False))


def split_text(text: str, language: str | None = None, min_chunks: int = 1) -> list[str]:
    tokens = tokenize(text, language)
    return [render_chunk(tokens[lo:hi], language) for lo, hi in split_ranges(tokens, language, None, min_chunks)]


class _Splitter:
    def __init__(self, tokens: list[Token], max_chars: int, pauses: list[float] | None, space_breaks: bool):
        self.tokens = tokens
        self.max_chars = max(1, max_chars)
        self.pauses = pauses
        self.space_breaks = space_breaks
        self.chars = [0]
        self.spaces = [0]
        for token in tokens:
            self.chars.append(self.chars[-1] + len(token.text))
            self.spaces.append(self.spaces[-1] + (1 if token.space_before else 0))

    def length(self, lo: int, hi: int) -> int:
        if hi <= lo:
            return 0
        return self.chars[hi] - self.chars[lo] + self.spaces[hi] - self.spaces[lo + 1]

    def split(self, lo: int, hi: int, chunks: int, grow: bool) -> list[tuple[int, int]]:
        length = self.length(lo, hi)
        if hi - lo <= 1:
            return [(lo, hi)]
        if chunks <= 1:
            if length <= self.max_chars or not grow:
                return [(lo, hi)]
            chunks = math.ceil(length / self.max_chars)
        left_chunks = chunks // 2
        right_chunks = chunks - left_chunks
        target = length * left_chunks / chunks
        shortest = length / chunks * SHORTEST_SHARE
        best_cut, best_score = None, None
        closest_cut, closest_gap = lo + 1, None
        for cut in range(lo + 1, hi):
            left = self.length(lo, cut)
            right = self.length(cut, hi)
            gap = abs(left - target)
            if closest_gap is None or gap < closest_gap:
                closest_cut, closest_gap = cut, gap
            if left > left_chunks * self.max_chars or right > right_chunks * self.max_chars:
                continue
            if left < shortest or right < shortest:
                continue
            score = gap / self.max_chars - self._break_bonus(cut)
            if best_score is None or score < best_score:
                best_cut, best_score = cut, score
        cut = best_cut if best_cut is not None else closest_cut
        return self.split(lo, cut, left_chunks, grow) + self.split(cut, hi, right_chunks, grow)

    def _break_bonus(self, cut: int) -> float:
        previous = self.tokens[cut - 1].text.rstrip(CLOSING_MARKS)[-1:]
        bonus = 0.0
        if previous and previous in SENTENCE_END:
            bonus += SENTENCE_BONUS
        elif previous and previous in CLAUSE_END:
            bonus += CLAUSE_BONUS
        elif self.space_breaks and self.tokens[cut].space_before:
            bonus += CLAUSE_BONUS
        if self.pauses and cut - 1 < len(self.pauses):
            bonus += PAUSE_BONUS * min(max(0.0, self.pauses[cut - 1]) / PAUSE_FULL_SECONDS, 1.0)
        return bonus


def _script_tokens(text: str) -> list[Token]:
    tokens: list[Token] = []
    space = False
    for match in SCRIPT_TOKEN_RE.finditer(text):
        piece = match.group(0)
        if piece.isspace():
            space = True
            continue
        if tokens and not space and unicodedata.category(piece[0]).startswith("M"):
            # A combining mark stays with its base character.
            tokens[-1].text += piece
            continue
        tokens.append(Token(piece, space and bool(tokens)))
        space = False
    return tokens


def _break_long_tokens(tokens: list[Token], max_chars: int) -> list[Token]:
    result: list[Token] = []
    for token in tokens:
        if len(token.text) <= max_chars:
            result.append(token)
            continue
        for offset in range(0, len(token.text), max_chars):
            result.append(Token(token.text[offset : offset + max_chars], token.space_before if offset == 0 else False))
    return result
