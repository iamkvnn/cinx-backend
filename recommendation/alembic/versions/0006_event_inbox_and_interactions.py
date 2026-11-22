"""Add the general event inbox, recommendation slates and idempotent client interactions.

Every step checks the live schema first, like 0004: ``user_interactions``
predates Alembic, and MySQL 8.0 has no ``ADD COLUMN IF NOT EXISTS``.
"""

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import mysql

# at most 32 characters: Alembic's version_num column is VARCHAR(32)
revision = "0006_event_inbox_interactions"
down_revision = "0003_feature_period_boundaries"
branch_labels = None
depends_on = None


def upgrade():
    inspector = sa.inspect(op.get_bind())
    tables = set(inspector.get_table_names())

    if "recommendation_event_inbox" not in tables:
        op.create_table(
            "recommendation_event_inbox",
            sa.Column("event_id", sa.String(128), primary_key=True),
            sa.Column("routing_key", sa.String(160), nullable=False),
            sa.Column("event_type", sa.String(100), nullable=True),
            sa.Column("aggregate_id", sa.String(128), nullable=True),
            sa.Column("processed_at", mysql.DATETIME(fsp=6), nullable=False),
        )
        op.create_index("idx_event_inbox_processed", "recommendation_event_inbox", ["processed_at"])

    if "recommendation_slate" not in tables:
        op.create_table(
            "recommendation_slate",
            sa.Column("id", sa.String(64), primary_key=True),
            sa.Column("user_id", sa.String(64), nullable=False),
            sa.Column("ranker_version", sa.String(64), nullable=False),
            sa.Column("course_ids", mysql.JSON(), nullable=False),
            sa.Column("created_at", mysql.DATETIME(fsp=6), nullable=False),
        )
        op.create_index("idx_recommendation_slate_user_created", "recommendation_slate", ["user_id", "created_at"])

    if "user_interactions" not in tables:
        op.create_table(
            "user_interactions",
            sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
            sa.Column("user_id", sa.String(64), nullable=False),
            sa.Column("course_id", sa.String(64), nullable=False),
            sa.Column("interaction_type", sa.String(50), nullable=False),
            sa.Column("weight", sa.Float(), nullable=False),
            sa.Column("created_at", sa.DateTime(), nullable=True),
        )
        op.create_index("idx_user_interactions_user", "user_interactions", ["user_id"])
        op.create_index("idx_user_interactions_course", "user_interactions", ["course_id"])
        op.create_index("idx_user_interactions_user_course", "user_interactions", ["user_id", "course_id"])

    columns = {column["name"] for column in sa.inspect(op.get_bind()).get_columns("user_interactions")}
    for column in (
        sa.Column("idempotency_key", sa.String(255), nullable=True),
        sa.Column("request_fingerprint", sa.String(64), nullable=True),
        sa.Column("recommendation_id", sa.String(64), nullable=True),
    ):
        if column.name not in columns:
            op.add_column("user_interactions", column)

    inspector = sa.inspect(op.get_bind())
    indexes = {index["name"] for index in inspector.get_indexes("user_interactions")}
    if "idx_user_interactions_user_created" not in indexes:
        op.create_index("idx_user_interactions_user_created", "user_interactions", ["user_id", "created_at"])
    if "idx_user_interactions_course_type" not in indexes:
        op.create_index("idx_user_interactions_course_type", "user_interactions", ["course_id", "interaction_type"])
    constraints = {constraint["name"] for constraint in inspector.get_unique_constraints("user_interactions")}
    if "uq_user_interactions_idempotency" not in constraints and "uq_user_interactions_idempotency" not in indexes:
        op.create_unique_constraint(
            "uq_user_interactions_idempotency", "user_interactions", ["user_id", "idempotency_key"]
        )


def downgrade():
    # Inbox, slate and interaction history are durable projection state; use a
    # forward migration for an approved cleanup.
    pass
