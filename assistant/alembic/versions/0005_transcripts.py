"""Lesson transcripts in the knowledge index: a course namespace, a content hash, and the re-index queue.

Revision ID: 0005_transcripts
Revises: 0004_action_revisions
"""

from alembic import op
import sqlalchemy as sa


revision = "0005_transcripts"
down_revision = "0004_action_revisions"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("knowledge_document", sa.Column("namespace", sa.String(64)))
    op.add_column("knowledge_document", sa.Column("content_hash", sa.String(64)))
    op.create_index("ix_knowledge_document_namespace", "knowledge_document", ["namespace"])
    op.create_table(
        "transcript_reindex",
        sa.Column("course_id", sa.String(64), primary_key=True),
        sa.Column("due_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("attempts", sa.Integer, nullable=False, server_default="0"),
    )
    op.create_index("ix_transcript_reindex_due_at", "transcript_reindex", ["due_at"])


def downgrade() -> None:
    op.drop_table("transcript_reindex")
    op.drop_index("ix_knowledge_document_namespace", table_name="knowledge_document")
    op.drop_column("knowledge_document", "content_hash")
    op.drop_column("knowledge_document", "namespace")
