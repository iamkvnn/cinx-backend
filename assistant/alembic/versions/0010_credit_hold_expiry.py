"""A credit hold can carry its own deadline, so a practice question outlives the chat-turn sweep window.

Revision ID: 0010_credit_hold_expiry
Revises: 0009_practice_existing_questions
"""

from alembic import op
import sqlalchemy as sa


revision = "0010_credit_hold_expiry"
down_revision = "0009_practice_existing_questions"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("credit_reservation", sa.Column("expires_at", sa.DateTime(timezone=True)))


def downgrade() -> None:
    op.drop_column("credit_reservation", "expires_at")
