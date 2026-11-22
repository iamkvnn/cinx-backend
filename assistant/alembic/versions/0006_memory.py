"""Learner memory: approved memories and the consent that allows proposing them.

Revision ID: 0006_memory
Revises: 0005_transcripts
"""

from alembic import op
import sqlalchemy as sa


revision = "0006_memory"
down_revision = "0005_transcripts"
branch_labels = None
depends_on = None

TS = sa.DateTime(timezone=True)


def upgrade() -> None:
    op.create_table(
        "learner_memory",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("kind", sa.String(20), nullable=False),
        sa.Column("text", sa.String(300), nullable=False),
        sa.Column("source_action_id", sa.String(36), unique=True),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("updated_at", TS, nullable=False),
        sa.Column("last_used_at", TS, nullable=False),
    )
    op.create_index("ix_learner_memory_user_used", "learner_memory", ["user_id", "last_used_at"])
    op.create_table(
        "memory_consent",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("granted_at", TS, nullable=False),
    )


def downgrade() -> None:
    op.drop_table("memory_consent")
    op.drop_table("learner_memory")
