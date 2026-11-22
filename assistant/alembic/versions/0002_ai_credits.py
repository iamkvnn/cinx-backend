"""AI credits: entitlement projection, inbox, credit counters and per-turn reservations.

Revision ID: 0002_ai_credits
Revises: 0001_baseline
"""

from alembic import op
import sqlalchemy as sa


revision = "0002_ai_credits"
down_revision = "0001_baseline"
branch_labels = None
depends_on = None

TS = sa.DateTime(timezone=True)


def upgrade() -> None:
    op.create_table(
        "user_entitlement",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("entitlement_version", sa.BigInteger, nullable=False),
        sa.Column("access_status", sa.String(20), nullable=False),
        sa.Column("reconcile_by", TS),
        sa.Column("access_effective_until", TS),
        sa.Column("received_at", TS, nullable=False),
    )
    op.create_table(
        "user_feature",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("feature_key", sa.String(160), primary_key=True),
        sa.Column("value_type", sa.String(16), nullable=False),
        sa.Column("enabled", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("quota_limit", sa.BigInteger),
        sa.Column("unlimited", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("unit", sa.String(16)),
        sa.Column("reset_policy", sa.String(32)),
        sa.Column("period_start", TS),
        sa.Column("period_end", TS),
        sa.Column("effective_until", TS),
    )
    op.create_table(
        "entitlement_inbox",
        sa.Column("event_id", sa.String(128), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("entitlement_version", sa.BigInteger, nullable=False),
        sa.Column("outcome", sa.String(16), nullable=False),
        sa.Column("received_at", TS, nullable=False),
    )
    op.create_table(
        "credit_counter",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("feature_key", sa.String(160), primary_key=True),
        sa.Column("period_key", sa.String(64), primary_key=True),
        sa.Column("used", sa.BigInteger, nullable=False, server_default="0"),
        sa.Column("reserved", sa.BigInteger, nullable=False, server_default="0"),
        sa.CheckConstraint("used >= 0 AND reserved >= 0", name="ck_credit_counter_non_negative"),
    )
    op.create_table(
        "credit_reservation",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("feature_key", sa.String(160), nullable=False),
        sa.Column("period_key", sa.String(64), nullable=False),
        sa.Column("amount", sa.Integer, nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column("charged", sa.Integer, nullable=False, server_default="0"),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("settled_at", TS),
    )
    op.create_index("ix_credit_reservation_status_created", "credit_reservation", ["status", "created_at"])
    op.add_column("agent_turn", sa.Column("reservation_id", sa.String(36)))
    op.add_column("agent_turn", sa.Column("credits_charged", sa.Integer, nullable=False, server_default="0"))


def downgrade() -> None:
    op.drop_column("agent_turn", "credits_charged")
    op.drop_column("agent_turn", "reservation_id")
    for table in ("credit_reservation", "credit_counter", "entitlement_inbox", "user_feature", "user_entitlement"):
        op.drop_table(table)
