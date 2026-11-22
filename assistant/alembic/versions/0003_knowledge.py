"""Knowledge: documents, embedded chunks (pgvector) and a generic event inbox.

Revision ID: 0003_knowledge
Revises: 0002_ai_credits

The vector column has no fixed dimension, so any embedding model fits. That rules out an HNSW
index; exact search is fine for the policy corpus and is revisited when transcripts are indexed.
"""

from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector


revision = "0003_knowledge"
down_revision = "0002_ai_credits"
branch_labels = None
depends_on = None

TS = sa.DateTime(timezone=True)


def upgrade() -> None:
    op.create_table(
        "knowledge_document",
        sa.Column("id", sa.String(160), primary_key=True),
        sa.Column("source_type", sa.String(32), nullable=False),
        sa.Column("title", sa.String(300), nullable=False),
        sa.Column("source_url", sa.String(500)),
        sa.Column("version", sa.Integer),
        sa.Column("published_at", TS),
        sa.Column("updated_at", TS, nullable=False),
    )
    op.create_table(
        "knowledge_chunk",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("document_id", sa.String(160), sa.ForeignKey("knowledge_document.id", ondelete="CASCADE"),
                  nullable=False),
        sa.Column("source_type", sa.String(32), nullable=False),
        sa.Column("ordinal", sa.Integer, nullable=False),
        sa.Column("content", sa.Text, nullable=False),
        sa.Column("embedding_model", sa.String(160), nullable=False),
        sa.Column("embedding", Vector(), nullable=False),
    )
    op.create_index("ix_knowledge_chunk_document_id", "knowledge_chunk", ["document_id"])
    op.create_index("ix_knowledge_chunk_source_model", "knowledge_chunk", ["source_type", "embedding_model"])
    op.create_table(
        "event_inbox",
        sa.Column("event_id", sa.String(128), primary_key=True),
        sa.Column("consumer", sa.String(80), nullable=False),
        sa.Column("received_at", TS, nullable=False),
    )


def downgrade() -> None:
    op.drop_table("event_inbox")
    op.drop_table("knowledge_chunk")
    op.drop_table("knowledge_document")
