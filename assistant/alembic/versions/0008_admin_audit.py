"""Admin copilot: an audit row for every tool it runs.

Revision ID: 0008_admin_audit
Revises: 0007_course_materials
"""

from alembic import op
import sqlalchemy as sa


revision = "0008_admin_audit"
down_revision = "0007_course_materials"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "admin_tool_audit",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("session_id", sa.String(36), nullable=False),
        sa.Column("capability_id", sa.String(80)),
        sa.Column("tool", sa.String(120), nullable=False),
        sa.Column("input_hash", sa.String(64), nullable=False),
        sa.Column("outcome", sa.String(20), nullable=False),
        sa.Column("mfa", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("occurred_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_admin_tool_audit_user_time", "admin_tool_audit", ["user_id", "occurred_at"])


def downgrade() -> None:
    op.drop_table("admin_tool_audit")
