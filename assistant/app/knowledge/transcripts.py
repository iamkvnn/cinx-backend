"""Lesson transcripts of published courses in the knowledge index, one document per subtitle track.

Learning announces `learning.course.transcripts.changed`; the course is queued, and once its delay passes the
whole course is read back from learning's internal API and reconciled: unchanged tracks (same sha256) are kept,
changed ones embedded again, and tracks that are gone, or a course no longer published, are dropped.
"""

import logging
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

import httpx
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.config import settings
from app.core.logging import outbound_trace_headers
from app.knowledge import subtitles
from app.knowledge.embedder import Embedder
from app.knowledge.entities import KnowledgeDocument, TranscriptReindex
from app.knowledge.service import KnowledgeService
from app.security.service_token import service_access_token


logger = logging.getLogger(__name__)
SOURCE_TYPE = "TRANSCRIPT"
LEARNING_INTERNAL_AUDIENCE = "cinx-learning-internal"
# a course that keeps failing is retried with growing waits, then left for the next change event
MAX_ATTEMPTS = 5

# course id -> learning's transcripts body ({courseId, published, lessons}); swapped for a fake in tests
FetchTranscripts = Callable[[str], Awaitable[dict]]


class TranscriptFetchError(Exception):
    """Learning could not answer now; the course stays queued."""


def document_id(course_id: str, lesson_id: str, track_id: str) -> str:
    return f"transcript:{course_id}:{lesson_id}:{track_id}"


def lesson_of(document: str) -> str | None:
    parts = document.split(":")
    return parts[2] if len(parts) == 4 and parts[0] == "transcript" else None


def passages(vtt: str, max_chars: int | None = None) -> list[str]:
    """Consecutive cues joined up to `max_chars`, each passage opened by its start as `[mm:ss]` for citations."""
    limit = max_chars or settings.TRANSCRIPT_PASSAGE_CHARS
    out: list[str] = []
    start, texts, size = None, [], 0
    for cue in subtitles.parse(vtt):
        if start is not None and size + len(cue.text) > limit:
            out.append(f"[{subtitles.clock(start)}] {' '.join(texts)}")
            start, texts, size = None, [], 0
        if start is None:
            start = cue.start_ms
        texts.append(cue.text)
        size += len(cue.text) + 1
    if start is not None:
        out.append(f"[{subtitles.clock(start)}] {' '.join(texts)}")
    return out


async def fetch_from_learning(course_id: str, transport: httpx.AsyncBaseTransport | None = None) -> dict:
    """GET /internal/courses/{id}/transcripts with the assistant's service token."""
    token = await service_access_token(LEARNING_INTERNAL_AUDIENCE)
    async with httpx.AsyncClient(base_url=settings.LEARNING_SERVICE_BASE_URL, transport=transport,
                                 timeout=settings.LEARNING_INTERNAL_TIMEOUT_SECONDS) as client:
        try:
            response = await client.get(f"/internal/courses/{course_id}/transcripts",
                                        headers={"Authorization": f"Bearer {token}", "Accept": "application/json",
                                                 **outbound_trace_headers()})
        except httpx.HTTPError as exc:
            raise TranscriptFetchError("learning is unreachable") from exc
    # a deleted course has nothing to index
    if response.status_code == 404:
        return {"courseId": course_id, "published": False, "lessons": []}
    if response.status_code >= 400:
        raise TranscriptFetchError(f"learning answered {response.status_code}")
    body = response.json()
    return body.get("data", body) if isinstance(body, dict) else {}


@dataclass(frozen=True)
class ReindexResult:
    embedded: int
    kept: int
    dropped: int


class TranscriptIndexer:
    def __init__(self, sessions: async_sessionmaker[AsyncSession], embedder: Embedder | None = None,
                 fetch: FetchTranscripts = fetch_from_learning):
        self.sessions = sessions
        self.embedder = embedder or Embedder()
        self.fetch = fetch

    async def reindex(self, course_id: str) -> ReindexResult:
        data = await self.fetch(course_id)
        async with self.sessions() as db:
            existing = {d.id: d.content_hash for d in (await db.scalars(select(KnowledgeDocument).where(
                KnowledgeDocument.namespace == course_id, KnowledgeDocument.source_type == SOURCE_TYPE))).all()}
        wanted: dict[str, tuple[dict, dict]] = {}
        if data.get("published"):
            for lesson in data.get("lessons") or []:
                for track in lesson.get("tracks") or []:
                    wanted[document_id(course_id, str(lesson["lessonId"]), str(track["trackId"]))] = (lesson, track)
        changed = {doc: pair for doc, pair in wanted.items() if existing.get(doc) != pair[1].get("sha256")}
        # embed every changed track before the write transaction opens: no network call while it holds rows
        embedded: list[tuple[str, dict, dict, list[str], list[list[float]]]] = []
        for doc, (lesson, track) in changed.items():
            pieces = passages(str(track.get("content") or ""))
            embedded.append((doc, lesson, track, pieces, await self.embedder.embed(pieces) if pieces else []))
        gone = [doc for doc in existing if doc not in wanted]
        async with self.sessions() as db:
            service = KnowledgeService(db, self.embedder)
            for doc, lesson, track, pieces, vectors in embedded:
                await service.write_embedded(
                    document_id=doc, source_type=SOURCE_TYPE, title=str(lesson.get("title") or ""),
                    source_url=f"/learning/{course_id}?lessonId={lesson['lessonId']}", namespace=course_id,
                    content_hash=track.get("sha256"), pieces=pieces, vectors=vectors)
            await service.drop_documents(gone)
            await db.commit()
        return ReindexResult(embedded=len(embedded), kept=len(wanted) - len(embedded), dropped=len(gone))


async def request_reindex(db: AsyncSession, course_id: str, now: datetime) -> None:
    """Queue a course; a course still waiting keeps its time, so a burst of events is one re-index.

    A course already due may be in the middle of its re-index, which read learning before this change:
    it is armed again, and that run then leaves the row for the next one.
    """
    due = now + timedelta(seconds=settings.TRANSCRIPT_REINDEX_DELAY_SECONDS)
    row = await db.get(TranscriptReindex, course_id)
    if row is None:
        db.add(TranscriptReindex(course_id=course_id, due_at=due, attempts=0))
    elif _aware(row.due_at) <= now:
        row.due_at, row.attempts = due, 0


def _aware(value: datetime) -> datetime:
    # SQLite hands back naive datetimes; Postgres keeps the zone.
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


async def reindex_due(sessions: async_sessionmaker[AsyncSession], indexer: TranscriptIndexer,
                      now: Callable[[], datetime] = lambda: datetime.now(timezone.utc), limit: int = 5) -> int:
    """Re-index the courses whose wait is over; a failure waits longer and is dropped after MAX_ATTEMPTS."""
    async with sessions() as db:
        due = [(row.course_id, _aware(row.due_at)) for row in (await db.scalars(
            select(TranscriptReindex).where(TranscriptReindex.due_at <= now()).order_by(TranscriptReindex.due_at).limit(limit))).all()]
    done = 0
    for course_id, picked_due in due:
        try:
            result = await indexer.reindex(course_id)
            logger.info("Transcripts re-indexed course=%s embedded=%s kept=%s dropped=%s",
                        course_id, result.embedded, result.kept, result.dropped)
            async with sessions() as db:
                row = await db.get(TranscriptReindex, course_id)
                # re-armed while this run read learning: keep it for the next run
                if row is not None and _aware(row.due_at) == picked_due:
                    await db.delete(row)
                await db.commit()
            done += 1
        except Exception:
            logger.exception("Transcript re-index failed course=%s", course_id)
            async with sessions() as db:
                row = await db.get(TranscriptReindex, course_id)
                if row is not None:
                    row.attempts += 1
                    if row.attempts >= MAX_ATTEMPTS:
                        await db.delete(row)
                    else:
                        row.due_at = now() + timedelta(seconds=settings.TRANSCRIPT_REINDEX_DELAY_SECONDS * 2 ** row.attempts)
                await db.commit()
    return done
