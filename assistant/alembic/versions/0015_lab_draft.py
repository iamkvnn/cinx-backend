"""CozX lab drafts: a proposal per request, kept so the Studio can poll it.

Revision ID: 0015_lab_draft
Revises: 0014_ai_suggestions
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0015_lab_draft"
down_revision = "0014_ai_suggestions"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "lab_draft",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("course_id", sa.String(64), nullable=False),
        sa.Column("lesson_id", sa.String(64), nullable=False),
        sa.Column("prompt", sa.Text, nullable=False),
        sa.Column("runtime_id", sa.String(30), nullable=False),
        sa.Column("mode", sa.String(10), nullable=False),
        sa.Column("status", sa.String(10), nullable=False),
        sa.Column("error", sa.String(60), nullable=True),
        sa.Column("reason", sa.Text, nullable=True),
        sa.Column("payload", postgresql.JSONB, nullable=True),
        sa.Column("model", sa.String(120), nullable=True),
        sa.Column("credits", sa.Integer, nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_lab_draft_user", "lab_draft", ["user_id", "created_at"])


def downgrade() -> None:
    op.drop_index("ix_lab_draft_user", table_name="lab_draft")
    op.drop_table("lab_draft")
