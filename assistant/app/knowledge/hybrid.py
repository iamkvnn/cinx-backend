"""Hybrid search over one course's transcripts: meaning (embeddings) and words (BM25), fused by reciprocal rank.

Embeddings miss rare terms (an error class, a function name); BM25 misses paraphrases. Both rankings are fused
the way recommendation's course search does it. A passage is kept only if it is close in meaning (cosine at least
TRANSCRIPT_MIN_SCORE) or contains a query term that is rare in the course, so a question the course does not cover
comes back empty instead of returning the least unrelated passages.
"""

import re
import unicodedata

from rank_bm25 import BM25Plus

from app.core.config import settings
from app.knowledge.service import TRANSCRIPT_SOURCE, KnowledgeService, Passage
from app.knowledge.transcripts import lesson_of


RRF_K = 60
CANDIDATE_POOL = 30
# a term in more of the course's passages than this says nothing about where the answer is ("chương trình", "dữ liệu")
RARE_SHARE = 0.2
RARE_MIN_PASSAGES = 2
VECTOR, KEYWORD, BOTH = "vector", "keyword", "both"

_TOKEN = re.compile(r"[a-z0-9]+")
_MARK = re.compile(r"^\s*\[(\d+:\d{2}(?::\d{2})?)\]")


def fold(text: str | None) -> str:
    """Lowercase without Vietnamese marks ("đ" -> "d"), so "lap trinh" matches "lập trình"."""
    lowered = str(text or "").lower().replace("đ", "d")
    return "".join(c for c in unicodedata.normalize("NFD", lowered) if unicodedata.category(c) != "Mn")


def words(text: str | None) -> list[str]:
    return _TOKEN.findall(fold(text))


def tokenize(text: str | None) -> list[str]:
    """Words plus adjacent-word bigrams: Vietnamese words are often two syllables."""
    found = words(text)
    return found + [f"{first}_{second}" for first, second in zip(found, found[1:])]


# Function words and words about the course itself ("bài nào nói về ..."), folded: they match every transcript.
STOPWORDS = frozenset(fold(word) for word in """
    là và của có không được này kia đó đây cho trong với một những các thì mà để khi nào sao như thế đã sẽ đang
    rất cũng ở về từ ra vào nếu vì nên hay hoặc nhưng còn ai gì bao nhiêu làm hỏi giúp mình tôi bạn em anh chị
    ơi nhé nha vậy ạ à chỗ cái người ta họ nó bị lại đến theo trên sau trước nữa chưa rồi mỗi cả chỉ thôi cách
    bài học khoá khóa video giảng viên nói đoạn phần chương nhắc tới đề cập giải thích hướng dẫn dạy
    a an the and or but of to in on at for with by from is are was were be been do does did have has it its this
    that these those what which who how why where when there here i you we they me my your can could would should
    will not no about into than then if as also any some all lesson lessons course courses video videos say says
    said talk mention mentioned explain explains cover covers covered teach teaches section please tell show find
""".split())


def key_terms(query: str) -> set[str]:
    """Query terms that may count as a keyword match: words that are not stopwords and have at least two characters,
    plus bigrams of two such words side by side."""
    found = words(query)
    keep = [len(word) > 1 and word not in STOPWORDS for word in found]
    singles = {word for word, kept in zip(found, keep) if kept}
    pairs = {f"{found[i]}_{found[i + 1]}" for i in range(len(found) - 1) if keep[i] and keep[i + 1]}
    return singles | pairs


def rare_terms(terms: set[str], moments: dict[tuple, set[str]]) -> set[str]:
    """The terms found at few enough moments of the course to point at one place in it.

    A moment is a lesson and its passage's opening mark, so the same moment in two language tracks counts once.
    """
    limit = max(RARE_MIN_PASSAGES, RARE_SHARE * len(moments))
    return {term for term in terms if sum(term in tokens for tokens in moments.values()) <= limit}


def moment_of(document_id: str, content: str) -> tuple[str | None, str]:
    mark = _MARK.match(content)
    return lesson_of(document_id), mark.group(1) if mark else content


def body(content: str) -> str:
    """The passage without its opening [mm:ss], so a timestamp never matches a query number."""
    return _MARK.sub("", content, count=1)


def fuse(rankings: list[list[int]]) -> list[int]:
    """Reciprocal rank fusion: ids by the sum of 1 / (RRF_K + rank) over the rankings that hold them."""
    fused: dict[int, float] = {}
    for ranked in rankings:
        for rank, item in enumerate(ranked, start=1):
            fused[item] = fused.get(item, 0.0) + 1.0 / (RRF_K + rank)
    return sorted(fused, key=lambda item: -fused[item])


def keyword_ranking(query: str, rows: list[tuple[int, str, str, str | None, str]]) -> list[int]:
    """Chunk ids holding a key term of the query that is rare in the course, best BM25 score first."""
    corpus = [(row[0], tokenize(body(row[4]))) for row in rows]
    corpus = [(chunk_id, tokens) for chunk_id, tokens in corpus if tokens]
    moments: dict[tuple, set[str]] = {}
    for row in rows:
        moments.setdefault(moment_of(row[1], row[4]), set()).update(tokenize(body(row[4])))
    terms = rare_terms(key_terms(query), moments)
    if not terms or not corpus:
        return []
    # BM25Plus keeps idf positive, which Okapi does not on a course of a few passages
    scores = BM25Plus([tokens for _, tokens in corpus]).get_scores([t for t in tokenize(query) if t not in STOPWORDS])
    hits = [(chunk_id, score) for (chunk_id, tokens), score in zip(corpus, scores) if terms & set(tokens)]
    return [chunk_id for chunk_id, _ in sorted(hits, key=lambda hit: -hit[1])]


async def search_transcripts(service: KnowledgeService, query: str, course_id: str, top_k: int = 5) -> list[Passage]:
    """Up to `top_k` passages of one course's transcripts, best first, each with its cosine and how it matched.

    A lesson indexed in several languages repeats a moment once per track; only the best-ranked one is kept.
    """
    vector = await service.embedder.embed_query(query)
    dense = await service.nearest(vector, TRANSCRIPT_SOURCE, CANDIDATE_POOL, course_id)
    rows = await service.chunk_texts(course_id, TRANSCRIPT_SOURCE) if key_terms(query) else []
    keyword = keyword_ranking(query, rows)[:CANDIDATE_POOL]
    info = {row[0]: row[1:] for row in rows}
    info.update({c.id: (d.id, d.title, d.source_url, c.content) for c, d, _ in dense})
    scores = {c.id: score for c, _, score in dense}
    scores.update(await service.cosines(vector, [chunk_id for chunk_id in keyword if chunk_id not in scores]))
    by_keyword = set(keyword)
    passages: list[Passage] = []
    seen: set[tuple[str | None, str]] = set()
    for chunk_id in fuse([[c.id for c, _, _ in dense], keyword]):
        score = scores.get(chunk_id, 0.0)
        close, worded = score >= settings.TRANSCRIPT_MIN_SCORE, chunk_id in by_keyword
        if not (close or worded):
            continue
        document_id, title, source_url, content = info[chunk_id]
        moment = moment_of(document_id, content)
        if moment in seen:
            continue
        seen.add(moment)
        passages.append(Passage(document_id, title, source_url, content, round(score, 4),
                                BOTH if close and worded else VECTOR if close else KEYWORD))
        if len(passages) >= top_k:
            break
    return passages
