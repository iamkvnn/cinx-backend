"""Practice jobs keep the quiz's questions as they were at the start, so generated ones avoid repeating them.

Revision ID: 0009_practice_existing_questions
Revises: 0008_admin_audit
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0009_practice_existing_questions"
down_revision = "0008_admin_audit"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("practice_job", sa.Column("existing_questions", postgresql.JSONB))


def downgrade() -> None:
    op.drop_column("practice_job", "existing_questions")
