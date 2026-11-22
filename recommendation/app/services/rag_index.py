import logging
import threading
import time
from dataclasses import dataclass
from functools import lru_cache

import faiss
import numpy as np
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.core.metrics import RAG_REBUILD_SECONDS, RAG_REBUILDS
from app.entities.course_chunk import CourseChunk
from app.entities.knowledge import KnowledgeChunk
from app.services.embedding_model import embed_documents
from app.services.lexical_index import lexical_course_index


logger = logging.getLogger(__name__)

# Course and policy changes arriving within this window share one rebuild.
REBUILD_DELAY_SECONDS = 3.0


@dataclass(frozen=True)
class IndexedChunk:
    id: str
    source_type: str
    content: str
    course_id: str
    lesson_ids: list[str]
    section_title: str | None = None
    document_id: str | None = None
    document_title: str | None = None
    source_url: str | None = None


@lru_cache(maxsize=1)
def _embedding_dimension() -> int:
    """The configured model's vector size, probed once per process."""
    return len(embed_documents(["embedding dimension probe"])[0])


class RAGIndex:
    """One FAISS index per source type, so a search never scans another type's chunks."""

    def __init__(self):
        self._lock = threading.RLock()
        self._indices: dict[str, tuple[faiss.Index, list[IndexedChunk]]] = {}
        # Rows of each course's chunks in the course index, to score one course
        # without re-embedding its text.
        self._course_positions: dict[str, list[int]] = {}
        # Which course a lesson belongs to, from the chunks' lesson ids.
        self._lesson_courses: dict[str, str] = {}

    def rebuild(self, db: Session) -> None:
        course_chunks = db.execute(select(CourseChunk)).scalars().all()
        knowledge_chunks = db.execute(select(KnowledgeChunk)).scalars().all()
        self._refresh_embeddings_if_needed(db, course_chunks, knowledge_chunks)

        grouped: dict[str, tuple[list[IndexedChunk], list[list[float]]]] = {}
        for chunk in course_chunks:
            if not chunk.embedding:
                continue
            chunks, vectors = grouped.setdefault("course", ([], []))
            chunks.append(
                IndexedChunk(
                    id=chunk.id,
                    source_type="course",
                    content=chunk.content,
                    course_id=chunk.course_id,
                    lesson_ids=chunk.lesson_ids or [],
                    section_title=chunk.section_title,
                )
            )
            vectors.append(chunk.embedding)

        for chunk in knowledge_chunks:
            if not chunk.embedding:
                continue
            source_type = chunk.document.source_type if chunk.document else "knowledge"
            chunks, vectors = grouped.setdefault(source_type, ([], []))
            chunks.append(
                IndexedChunk(
                    id=chunk.id,
                    source_type=source_type,
                    content=chunk.content,
                    course_id="",
                    lesson_ids=[],
                    document_id=chunk.document_id,
                    document_title=chunk.document.title if chunk.document else None,
                    source_url=chunk.document.source_url if chunk.document else None,
                )
            )
            vectors.append(chunk.embedding)

        indices: dict[str, tuple[faiss.Index, list[IndexedChunk]]] = {}
        for source_type, (chunks, vectors) in grouped.items():
            matrix = np.array(vectors, dtype="float32")
            if matrix.ndim != 2:
                logger.warning("RAG FAISS index skipped source_type=%s due to invalid embedding shape", source_type)
                continue
            faiss.normalize_L2(matrix)
            index = faiss.IndexFlatIP(matrix.shape[1])
            index.add(matrix)
            indices[source_type] = (index, chunks)

        course_positions: dict[str, list[int]] = {}
        lesson_courses: dict[str, str] = {}
        for position, chunk in enumerate(indices.get("course", (None, []))[1]):
            course_positions.setdefault(chunk.course_id, []).append(position)
            for lesson_id in chunk.lesson_ids:
                lesson_courses[lesson_id] = chunk.course_id

        with self._lock:
            self._indices = indices
            self._course_positions = course_positions
            self._lesson_courses = lesson_courses
        logger.info(
            "RAG FAISS indices rebuilt chunk_counts=%s",
            {source_type: len(chunks) for source_type, (_, chunks) in indices.items()},
        )

    def _refresh_embeddings_if_needed(
        self,
        db: Session,
        course_chunks: list[CourseChunk],
        knowledge_chunks: list[KnowledgeChunk],
    ) -> None:
        chunks = [chunk for chunk in [*course_chunks, *knowledge_chunks] if chunk.content]
        if not chunks:
            return

        expected_dimension = _embedding_dimension()
        # Only chunks without a vector of the current model's size are embedded;
        # one new chunk no longer re-embeds the whole corpus.
        stale = [chunk for chunk in chunks if not chunk.embedding or len(chunk.embedding) != expected_dimension]
        if not stale:
            return

        logger.info(
            "Refreshing RAG embeddings for current model chunk_count=%s total=%s dimension=%s",
            len(stale),
            len(chunks),
            expected_dimension,
        )
        embeddings = embed_documents([chunk.content for chunk in stale])
        for chunk, embedding in zip(stale, embeddings):
            chunk.embedding = embedding
        db.commit()

    def search(
        self,
        query_embedding: list[float],
        top_k: int,
        source_type: str | None = None,
    ) -> list[tuple[IndexedChunk, float]]:
        with self._lock:
            if source_type is None:
                targets = list(self._indices.values())
            else:
                target = self._indices.get(source_type)
                targets = [target] if target else []

        results: list[tuple[IndexedChunk, float]] = []
        for index, chunks in targets:
            query = np.array([query_embedding], dtype="float32")
            if query.shape[1] != index.d:
                logger.warning(
                    "RAG query embedding dimension mismatch expected=%s actual=%s",
                    index.d,
                    query.shape[1],
                )
                continue
            faiss.normalize_L2(query)
            scores, indices = index.search(query, min(top_k, len(chunks)))
            for score, position in zip(scores[0], indices[0]):
                if position >= 0:
                    results.append((chunks[position], float(score)))
        results.sort(key=lambda item: item[1], reverse=True)
        return results[:top_k]

    def course_chunk_scores(self, query_embedding: list[float], course_id: str) -> list[tuple[IndexedChunk, float]]:
        """Every indexed chunk of one course scored against the query, best first."""
        with self._lock:
            entry = self._indices.get("course")
            positions = list(self._course_positions.get(course_id, []))
        if entry is None or not positions:
            return []
        index, chunks = entry
        query = np.array([query_embedding], dtype="float32")
        if query.shape[1] != index.d:
            return []
        faiss.normalize_L2(query)
        # Stored vectors are already normalised, so the dot product is the cosine.
        vectors = np.vstack([index.reconstruct(position) for position in positions])
        scores = vectors @ query[0]
        ranked = sorted(zip(positions, scores), key=lambda item: float(item[1]), reverse=True)
        return [(chunks[position], float(score)) for position, score in ranked]

    def course_for_lesson(self, lesson_id: str) -> str | None:
        with self._lock:
            return self._lesson_courses.get(lesson_id)

    def course_vector(self, course_id: str) -> np.ndarray | None:
        """A course's normalised vector: its summary chunk, else the mean of its chunks."""
        with self._lock:
            entry = self._indices.get("course")
            positions = list(self._course_positions.get(course_id, []))
        if entry is None or not positions:
            return None
        index, chunks = entry
        summary = [position for position in positions if chunks[position].section_title is None]
        vector = np.mean(np.vstack([index.reconstruct(position) for position in summary or positions]), axis=0)
        norm = float(np.linalg.norm(vector))
        return vector / norm if norm else None


rag_index = RAGIndex()


def rebuild_rag_index(db: Session) -> None:
    started = time.perf_counter()
    try:
        rag_index.rebuild(db)
        lexical_course_index.rebuild(db)
    except Exception:
        RAG_REBUILDS.labels("error").inc()
        raise
    RAG_REBUILDS.labels("ok").inc()
    RAG_REBUILD_SECONDS.observe(time.perf_counter() - started)


class RebuildScheduler:
    """Coalesces index rebuild requests.

    The first request starts a timer; requests until it fires share that
    rebuild, and requests during a rebuild cause exactly one more.  Each
    rebuild opens its own database session.
    """

    def __init__(self, delay_seconds: float = REBUILD_DELAY_SECONDS):
        self.delay_seconds = delay_seconds
        self._lock = threading.Lock()
        self._timer: threading.Timer | None = None
        self._running = False
        self._dirty = False

    def request(self) -> None:
        with self._lock:
            if self._running:
                self._dirty = True
                return
            if self._timer is not None:
                return
            self._timer = threading.Timer(self.delay_seconds, self._run)
            self._timer.daemon = True
            self._timer.start()

    def _run(self) -> None:
        with self._lock:
            self._timer = None
            self._running = True
            self._dirty = False
        try:
            db = SessionLocal()
            try:
                rebuild_rag_index(db)
            finally:
                db.close()
        except Exception:
            logger.exception("Scheduled RAG index rebuild failed")
        finally:
            with self._lock:
                self._running = False
                again = self._dirty
                self._dirty = False
            if again:
                self.request()


rebuild_scheduler = RebuildScheduler()


def schedule_rag_rebuild() -> None:
    """Rebuild the indexes shortly, in the background, sharing one rebuild across a burst of changes."""
    rebuild_scheduler.request()
