"""Draft history: the payload of a pending action at each version, for the workspace's "earlier version".

Revision ID: 0004_action_revisions
Revises: 0003_knowledge
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0004_action_revisions"
down_revision = "0003_knowledge"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "action_revision",
        sa.Column("pending_action_id", sa.String(36), sa.ForeignKey("pending_action.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("version", sa.Integer, primary_key=True),
        sa.Column("payload", postgresql.JSONB, nullable=False),
        sa.Column("preview", postgresql.JSONB, nullable=False),
        sa.Column("author", sa.String(20), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    # Drafts proposed before this table existed get their current version as the first entry.
    op.execute(
        "INSERT INTO action_revision (pending_action_id, version, payload, preview, author, created_at) "
        "SELECT id, version, payload, preview, 'ASSISTANT', created_at FROM pending_action"
    )


def downgrade() -> None:
    op.drop_table("action_revision")
