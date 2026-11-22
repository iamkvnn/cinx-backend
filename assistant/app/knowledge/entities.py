from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import JSON, DateTime, ForeignKey, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


# pgvector in Postgres; a JSON list on SQLite so unit tests run without the extension.
Embedding = Vector().with_variant(JSON(), "sqlite")


class KnowledgeDocument(Base):
    __tablename__ = "knowledge_document"

    id: Mapped[str] = mapped_column(String(160), primary_key=True)
    source_type: Mapped[str] = mapped_column(String(32))
    title: Mapped[str] = mapped_column(String(300))
    source_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    version: Mapped[int | None] = mapped_column(Integer, nullable=True)
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    # the course a transcript belongs to; search never crosses it. Null for policies, which are global.
    namespace: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    # sha256 of the source text, so an unchanged transcript is not embedded again
    content_hash: Mapped[str | None] = mapped_column(String(64), nullable=True)


class KnowledgeChunk(Base):
    __tablename__ = "knowledge_chunk"
    __table_args__ = (Index("ix_knowledge_chunk_source_model", "source_type", "embedding_model"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    document_id: Mapped[str] = mapped_column(ForeignKey("knowledge_document.id", ondelete="CASCADE"), index=True)
    source_type: Mapped[str] = mapped_column(String(32))
    ordinal: Mapped[int] = mapped_column(Integer)
    content: Mapped[str] = mapped_column(Text)
    # Vectors from different models are not comparable: search only rows of the configured model.
    embedding_model: Mapped[str] = mapped_column(String(160))
    embedding: Mapped[list[float]] = mapped_column(Embedding)


class TranscriptReindex(Base):
    """A course whose transcripts changed; re-indexed once `due_at` passes, so a burst of edits costs one read."""

    __tablename__ = "transcript_reindex"

    course_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    attempts: Mapped[int] = mapped_column(Integer, default=0)


class EventInbox(Base):
    """Events a consumer already applied, keyed by eventId, for at-least-once delivery."""

    __tablename__ = "event_inbox"

    event_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    consumer: Mapped[str] = mapped_column(String(80))
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
