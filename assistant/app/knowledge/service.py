"""Knowledge documents (policies today) chunked, embedded and searched by meaning."""

import re
from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.knowledge.embedder import Embedder, cosine
from app.knowledge.entities import KnowledgeChunk, KnowledgeDocument


CHUNK_CHARS = 1200
CHUNK_OVERLAP = 150
# transcripts.SOURCE_TYPE; repeated here because transcripts imports this module
TRANSCRIPT_SOURCE = "TRANSCRIPT"


@dataclass(frozen=True)
class Passage:
    document_id: str
    title: str
    source_url: str | None
    content: str
    score: float
    # transcript search only: "vector", "keyword" or "both"
    matched_by: str | None = None


def chunk(text: str, size: int = CHUNK_CHARS, overlap: int = CHUNK_OVERLAP) -> list[str]:
    """Character windows with overlap, the same split recommendation used."""
    text = re.sub(r"\s+", " ", text or "").strip()
    if not text:
        return []
    pieces, start = [], 0
    while start < len(text):
        pieces.append(text[start:start + size])
        if start + size >= len(text):
            break
        start += size - overlap
    return pieces


class KnowledgeService:
    def __init__(self, db: AsyncSession, embedder: Embedder | None = None):
        self.db = db
        self.embedder = embedder or Embedder()

    async def replace_document(self, *, document_id: str, source_type: str, title: str, content: str,
                               source_url: str | None, version: int | None, published_at: datetime | None) -> int:
        """Replace a document's chunks; an older version than the stored one changes nothing. Returns chunk count."""
        pieces = chunk(f"{title}\n\n{content}")
        # Embed before the first query opens the transaction: no network call while it holds rows.
        vectors = await self.embedder.embed(pieces)
        existing = await self.db.get(KnowledgeDocument, document_id)
        if existing is not None and version is not None and existing.version is not None and version < existing.version:
            return 0
        if existing is None:
            existing = KnowledgeDocument(id=document_id)
            self.db.add(existing)
        existing.source_type, existing.title, existing.source_url = source_type, title, source_url
        existing.version, existing.published_at = version, published_at
        existing.updated_at = datetime.now(timezone.utc)
        await self.db.flush()
        await self.db.execute(delete(KnowledgeChunk).where(KnowledgeChunk.document_id == document_id))
        for ordinal, (piece, vector) in enumerate(zip(pieces, vectors)):
            self.db.add(KnowledgeChunk(document_id=document_id, source_type=source_type, ordinal=ordinal,
                                       content=piece, embedding_model=self.embedder.model, embedding=vector))
        return len(pieces)

    async def archive_document(self, document_id: str) -> bool:
        document = await self.db.get(KnowledgeDocument, document_id)
        if document is None:
            return False
        await self.db.execute(delete(KnowledgeChunk).where(KnowledgeChunk.document_id == document_id))
        await self.db.delete(document)
        return True

    async def write_embedded(self, *, document_id: str, source_type: str, title: str, source_url: str | None,
                             namespace: str | None, content_hash: str | None, pieces: list[str], vectors: list[list[float]]) -> None:
        """Replace a document with chunks already embedded (the caller embeds outside the transaction)."""
        document = await self.db.get(KnowledgeDocument, document_id)
        if document is None:
            document = KnowledgeDocument(id=document_id)
            self.db.add(document)
        document.source_type, document.title, document.source_url = source_type, title, source_url
        document.namespace, document.content_hash = namespace, content_hash
        document.version, document.published_at, document.updated_at = None, None, datetime.now(timezone.utc)
        await self.db.flush()
        await self.db.execute(delete(KnowledgeChunk).where(KnowledgeChunk.document_id == document_id))
        for ordinal, (piece, vector) in enumerate(zip(pieces, vectors)):
            self.db.add(KnowledgeChunk(document_id=document_id, source_type=source_type, ordinal=ordinal,
                                       content=piece, embedding_model=self.embedder.model, embedding=vector))

    async def drop_documents(self, document_ids: list[str]) -> None:
        if not document_ids:
            return
        await self.db.execute(delete(KnowledgeChunk).where(KnowledgeChunk.document_id.in_(document_ids)))
        await self.db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.id.in_(document_ids)))

    async def search(self, query: str, source_type: str, top_k: int = 5, namespace: str | None = None) -> list[Passage]:
        """Passages by meaning; with a namespace (a course), only that course's documents are searched.

        A course's transcripts go through hybrid search (meaning and keywords, see `hybrid.py`) with its own floor.
        """
        if source_type == TRANSCRIPT_SOURCE and namespace is not None:
            from app.knowledge.hybrid import search_transcripts  # hybrid builds on this module

            return await search_transcripts(self, query, namespace, top_k)
        vector = await self.embedder.embed_query(query)
        return [
            Passage(document.id, document.title, document.source_url, chunk_row.content, round(score, 4))
            for chunk_row, document, score in await self.nearest(vector, source_type, top_k, namespace)
            if score >= settings.KNOWLEDGE_MIN_SCORE
        ]

    async def nearest(self, vector: list[float], source_type: str, limit: int,
                      namespace: str | None = None) -> list[tuple[KnowledgeChunk, KnowledgeDocument, float]]:
        """Chunks closest to `vector` with their cosine, best first; no floor applied."""
        base = (
            select(KnowledgeChunk, KnowledgeDocument)
            .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
            .where(KnowledgeChunk.source_type == source_type, KnowledgeChunk.embedding_model == self.embedder.model)
        )
        if namespace is not None:
            base = base.where(KnowledgeDocument.namespace == namespace)
        if self.db.bind.dialect.name == "postgresql":
            distance = KnowledgeChunk.embedding.cosine_distance(vector)
            rows = (await self.db.execute(base.add_columns(distance).order_by(distance).limit(limit))).all()
            return [(chunk_row, document, 1.0 - float(dist)) for chunk_row, document, dist in rows]
        rows = (await self.db.execute(base)).all()
        return sorted(((c, d, cosine(vector, c.embedding)) for c, d in rows), key=lambda r: -r[2])[:limit]

    async def chunk_texts(self, namespace: str, source_type: str) -> list[tuple[int, str, str, str | None, str]]:
        """(chunk id, document id, title, source url, content) of every chunk in a namespace, without vectors."""
        rows = await self.db.execute(
            select(KnowledgeChunk.id, KnowledgeChunk.document_id, KnowledgeDocument.title, KnowledgeDocument.source_url,
                   KnowledgeChunk.content)
            .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
            .where(KnowledgeDocument.namespace == namespace, KnowledgeChunk.source_type == source_type,
                   KnowledgeChunk.embedding_model == self.embedder.model)
            .order_by(KnowledgeChunk.document_id, KnowledgeChunk.ordinal))
        return [tuple(row) for row in rows.all()]

    async def cosines(self, vector: list[float], chunk_ids: list[int]) -> dict[int, float]:
        """Cosine between `vector` and each of these chunks."""
        if not chunk_ids:
            return {}
        if self.db.bind.dialect.name == "postgresql":
            distance = KnowledgeChunk.embedding.cosine_distance(vector)
            rows = await self.db.execute(select(KnowledgeChunk.id, distance).where(KnowledgeChunk.id.in_(chunk_ids)))
            return {chunk_id: 1.0 - float(dist) for chunk_id, dist in rows.all()}
        rows = await self.db.execute(select(KnowledgeChunk.id, KnowledgeChunk.embedding).where(KnowledgeChunk.id.in_(chunk_ids)))
        return {chunk_id: cosine(vector, embedding) for chunk_id, embedding in rows.all()}
