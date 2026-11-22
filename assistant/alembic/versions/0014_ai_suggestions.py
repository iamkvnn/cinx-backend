"""Background suggestions made from domain events, and the per-course and per-assignment switches that gate them.

Revision ID: 0014_ai_suggestions
Revises: 0013_turn_events
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0014_ai_suggestions"
down_revision = "0013_turn_events"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "ai_suggestion",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("kind", sa.String(30), nullable=False),
        sa.Column("subject_type", sa.String(30), nullable=False),
        sa.Column("subject_id", sa.String(64), nullable=False),
        sa.Column("course_id", sa.String(64), nullable=False),
        sa.Column("scope_id", sa.String(64), nullable=True),
        sa.Column("owner_user_id", sa.String(64), nullable=True),
        sa.Column("actor_id", sa.String(64), nullable=True),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("skip_reason", sa.String(40), nullable=True),
        sa.Column("payload", postgresql.JSONB, nullable=True),
        sa.Column("model", sa.String(120), nullable=True),
        sa.Column("credits", sa.Integer, nullable=False, server_default="0"),
        sa.Column("outcome", sa.String(20), nullable=True),
        sa.Column("outcome_by", sa.String(64), nullable=True),
        sa.Column("error", sa.Text, nullable=True),
        sa.Column("source_event_id", sa.String(64), nullable=False, unique=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_ai_suggestion_subject", "ai_suggestion", ["kind", "subject_id"])
    op.create_index("ix_ai_suggestion_owner_day", "ai_suggestion", ["owner_user_id", "kind", "created_at"])
    op.create_table(
        "ai_automation_setting",
        sa.Column("scope_type", sa.String(20), primary_key=True),
        sa.Column("scope_id", sa.String(64), primary_key=True),
        sa.Column("kind", sa.String(30), primary_key=True),
        sa.Column("enabled", sa.Boolean, nullable=False),
        sa.Column("updated_by", sa.String(64), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )


def downgrade() -> None:
    op.drop_table("ai_automation_setting")
    op.drop_index("ix_ai_suggestion_owner_day", table_name="ai_suggestion")
    op.drop_index("ix_ai_suggestion_subject", table_name="ai_suggestion")
    op.drop_table("ai_suggestion")
