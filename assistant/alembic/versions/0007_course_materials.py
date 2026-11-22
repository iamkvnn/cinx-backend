"""Instructor course materials whose extracted text the copilot cites, and practice question jobs.

Revision ID: 0007_course_materials
Revises: 0006_memory
"""

from alembic import op
import sqlalchemy as sa


revision = "0007_course_materials"
down_revision = "0006_memory"
branch_labels = None
depends_on = None

TS = sa.DateTime(timezone=True)


def upgrade() -> None:
    op.create_table(
        "course_material",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("course_id", sa.String(64), nullable=False),
        sa.Column("uploaded_by", sa.String(64), nullable=False),
        sa.Column("file_key", sa.String(300), nullable=False),
        sa.Column("file_name", sa.String(200), nullable=False),
        sa.Column("content_type", sa.String(120), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("error", sa.Text),
        sa.Column("units", sa.Integer, nullable=False, server_default="0"),
        sa.Column("attempts", sa.Integer, nullable=False, server_default="0"),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("updated_at", TS, nullable=False),
    )
    op.create_index("ix_course_material_course", "course_material", ["course_id"])
    op.create_index("ix_course_material_status", "course_material", ["status"])
    op.create_table(
        "practice_job",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("course_id", sa.String(64), nullable=False),
        sa.Column("quiz_lesson_id", sa.String(64), nullable=False),
        sa.Column("session_id", sa.String(36), nullable=False),
        sa.Column("topic", sa.String(300)),
        sa.Column("requested", sa.Integer, nullable=False),
        sa.Column("produced", sa.Integer, nullable=False, server_default="0"),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("error", sa.Text),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("updated_at", TS, nullable=False),
    )
    op.create_index("ix_practice_job_user_course", "practice_job", ["user_id", "course_id"])


def downgrade() -> None:
    op.drop_table("practice_job")
    op.drop_table("course_material")
