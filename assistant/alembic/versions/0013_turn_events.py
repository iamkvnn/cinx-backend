"""Every event of a turn in order, so a turn runs on when its client leaves and a client can resume it.

Revision ID: 0013_turn_events
Revises: 0012_context_compaction
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0013_turn_events"
down_revision = "0012_context_compaction"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "agent_turn_event",
        sa.Column("turn_id", sa.String(36), sa.ForeignKey("agent_turn.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("seq", sa.Integer, primary_key=True),
        sa.Column("event", sa.String(40), nullable=False),
        sa.Column("data", postgresql.JSONB, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )


def downgrade() -> None:
    op.drop_table("agent_turn_event")
