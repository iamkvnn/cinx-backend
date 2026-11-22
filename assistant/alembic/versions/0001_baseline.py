"""Baseline: sessions, turns, pending actions and their audit, governance, outbox.

Revision ID: 0001_baseline
Revises:
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision = "0001_baseline"
down_revision = None
branch_labels = None
depends_on = None

TS = sa.DateTime(timezone=True)


def upgrade() -> None:
    # pgvector is enabled now so the knowledge index (phase 1) needs no privileged migration later.
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "agent_session",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(64), nullable=False),
        sa.Column("persona", sa.String(20), nullable=False),
        sa.Column("placement", sa.String(40), nullable=False),
        sa.Column("title", sa.String(200)),
        sa.Column("last_capability_id", sa.String(80)),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("updated_at", TS, nullable=False),
    )
    op.create_index("ix_agent_session_user_updated", "agent_session", ["user_id", "updated_at"])

    op.create_table(
        "agent_turn",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("session_id", sa.String(36), sa.ForeignKey("agent_session.id", ondelete="CASCADE"), nullable=False),
        sa.Column("idempotency_key", sa.String(120), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("user_message", sa.Text, nullable=False),
        sa.Column("answer", sa.Text),
        sa.Column("capability_id", sa.String(80)),
        sa.Column("model_messages", postgresql.JSONB),
        sa.Column("events", postgresql.JSONB),
        sa.Column("input_tokens", sa.Integer, nullable=False, server_default="0"),
        sa.Column("output_tokens", sa.Integer, nullable=False, server_default="0"),
        sa.Column("error_code", sa.String(80)),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("finished_at", TS),
        sa.UniqueConstraint("session_id", "idempotency_key", name="uq_agent_turn_idempotency"),
    )
    op.create_index("ix_agent_turn_session_created", "agent_turn", ["session_id", "created_at"])

    op.create_table(
        "pending_action",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("type", sa.String(80), nullable=False),
        sa.Column("version", sa.Integer, nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("actor_user_id", sa.String(64), nullable=False),
        sa.Column("persona", sa.String(20), nullable=False),
        sa.Column("capability_id", sa.String(80), nullable=False),
        sa.Column("session_id", sa.String(36)),
        sa.Column("target_service", sa.String(40), nullable=False),
        sa.Column("payload", postgresql.JSONB, nullable=False),
        sa.Column("preview", postgresql.JSONB, nullable=False),
        sa.Column("approver_rule", sa.String(20), nullable=False),
        sa.Column("approver_permission", sa.String(120)),
        sa.Column("requires_mfa", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("idempotency_key", sa.String(120), nullable=False, unique=True),
        sa.Column("result", postgresql.JSONB),
        sa.Column("error_code", sa.String(80)),
        sa.Column("error_detail", sa.Text),
        sa.Column("expires_at", TS, nullable=False),
        sa.Column("created_at", TS, nullable=False),
        sa.Column("decided_at", TS),
        sa.Column("decided_by", sa.String(64)),
        sa.Column("decision_reason", sa.Text),
        sa.Column("executed_at", TS),
    )
    op.create_index("ix_pending_action_actor_status", "pending_action", ["actor_user_id", "status"])
    op.create_index("ix_pending_action_status_expires", "pending_action", ["status", "expires_at"])

    op.create_table(
        "action_audit",
        sa.Column("id", sa.BigInteger, primary_key=True, autoincrement=True),
        sa.Column("pending_action_id", sa.String(36), sa.ForeignKey("pending_action.id"), nullable=False),
        sa.Column("event", sa.String(30), nullable=False),
        sa.Column("actor_user_id", sa.String(64), nullable=False),
        sa.Column("on_behalf_of", sa.String(64), nullable=False),
        sa.Column("payload_hash", sa.String(64), nullable=False),
        sa.Column("outcome_code", sa.String(80)),
        sa.Column("detail", sa.Text),
        sa.Column("occurred_at", TS, nullable=False),
    )
    op.create_index("ix_action_audit_pending_action_id", "action_audit", ["pending_action_id"])
    # The audit trail is append-only: the service role may insert but never change history.
    op.execute(
        "CREATE FUNCTION action_audit_append_only() RETURNS trigger AS $$ "
        "BEGIN RAISE EXCEPTION 'action_audit is append-only'; END; $$ LANGUAGE plpgsql"
    )
    op.execute(
        "CREATE TRIGGER action_audit_no_change BEFORE UPDATE OR DELETE ON action_audit "
        "FOR EACH ROW EXECUTE FUNCTION action_audit_append_only()"
    )

    op.create_table(
        "capability_state",
        sa.Column("capability_id", sa.String(80), primary_key=True),
        sa.Column("enabled", sa.Boolean, nullable=False),
        sa.Column("updated_by", sa.String(64), nullable=False),
        sa.Column("reason", sa.String(500)),
        sa.Column("updated_at", TS, nullable=False),
    )
    op.create_table(
        "capability_usage",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("capability_id", sa.String(80), primary_key=True),
        sa.Column("usage_day", sa.Date, primary_key=True),
        sa.Column("turns", sa.Integer, nullable=False, server_default="0"),
    )
    op.create_table(
        "ai_policy_acceptance",
        sa.Column("user_id", sa.String(64), primary_key=True),
        sa.Column("policy_version", sa.Integer, primary_key=True),
        sa.Column("accepted_at", TS, nullable=False),
    )

    op.create_table(
        "outbox_event",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("event_type", sa.String(120), nullable=False),
        sa.Column("event_version", sa.Integer, nullable=False),
        sa.Column("routing_key", sa.String(120), nullable=False),
        sa.Column("aggregate_type", sa.String(60), nullable=False),
        sa.Column("aggregate_id", sa.String(64), nullable=False),
        sa.Column("payload", postgresql.JSONB, nullable=False),
        sa.Column("trace_id", sa.String(32)),
        sa.Column("occurred_at", TS, nullable=False),
        sa.Column("published_at", TS),
        sa.Column("attempts", sa.Integer, nullable=False, server_default="0"),
        sa.Column("last_error", sa.Text),
    )
    op.create_index("ix_outbox_event_unpublished", "outbox_event", ["published_at", "occurred_at"])


def downgrade() -> None:
    for table in ("outbox_event", "ai_policy_acceptance", "capability_usage", "capability_state"):
        op.drop_table(table)
    op.execute("DROP TRIGGER IF EXISTS action_audit_no_change ON action_audit")
    op.execute("DROP FUNCTION IF EXISTS action_audit_append_only()")
    for table in ("action_audit", "pending_action", "agent_turn", "agent_session"):
        op.drop_table(table)
