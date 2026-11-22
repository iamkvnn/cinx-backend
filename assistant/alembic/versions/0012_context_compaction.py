"""How full the model's context was after each turn, and the summary a compacted conversation keeps.

Revision ID: 0012_context_compaction
Revises: 0011_answer_reports
"""

from alembic import op
import sqlalchemy as sa


revision = "0012_context_compaction"
down_revision = "0011_answer_reports"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("agent_turn", sa.Column("context_tokens", sa.Integer))
    op.add_column("agent_turn", sa.Column("context_limit", sa.Integer))
    op.add_column("agent_session", sa.Column("summary", sa.Text))
    op.add_column("agent_session", sa.Column("summary_turn_id", sa.String(36)))
    op.add_column("agent_session", sa.Column("compacted_messages", sa.Integer, nullable=False, server_default="0"))


def downgrade() -> None:
    op.drop_column("agent_session", "compacted_messages")
    op.drop_column("agent_session", "summary_turn_id")
    op.drop_column("agent_session", "summary")
    op.drop_column("agent_turn", "context_limit")
    op.drop_column("agent_turn", "context_tokens")
