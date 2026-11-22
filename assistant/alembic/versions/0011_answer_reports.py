"""Tutor modes on a turn, and learners' reports of wrong answers with a copy of the turn they report.

Revision ID: 0011_answer_reports
Revises: 0010_credit_hold_expiry
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0011_answer_reports"
down_revision = "0010_credit_hold_expiry"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("agent_turn", sa.Column("mode", sa.String(30)))
    op.add_column("agent_turn", sa.Column("model", sa.String(120)))
    op.add_column("agent_turn", sa.Column("context", postgresql.JSONB))
    op.create_table(
        "answer_report",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("turn_id", sa.String(36), nullable=False),
        sa.Column("session_id", sa.String(36), nullable=False),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("capability_id", sa.String(80)),
        sa.Column("mode", sa.String(30)),
        sa.Column("course_id", sa.String(64)),
        sa.Column("lesson_id", sa.String(64)),
        sa.Column("reason", sa.String(30), nullable=False),
        sa.Column("note", sa.Text),
        sa.Column("user_message", sa.Text, nullable=False),
        sa.Column("answer", sa.Text, nullable=False),
        sa.Column("model", sa.String(120)),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("reviewed_by", sa.String(64)),
        sa.Column("reviewed_at", sa.DateTime(timezone=True)),
        sa.UniqueConstraint("turn_id", "user_id", name="uq_answer_report_turn_user"),
    )
    op.create_index("ix_answer_report_status_created", "answer_report", ["status", "created_at"])


def downgrade() -> None:
    op.drop_index("ix_answer_report_status_created", table_name="answer_report")
    op.drop_table("answer_report")
    op.drop_column("agent_turn", "context")
    op.drop_column("agent_turn", "model")
    op.drop_column("agent_turn", "mode")
