"""Add entitlement projections, inbox and recommendation quota ledger."""

import sqlalchemy as sa
from alembic import op

revision = "0002_entitlement_quota"
down_revision = "0001_baseline"
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_user_entitlement_projection (
          user_id VARCHAR(64) NOT NULL PRIMARY KEY,
          subject_type VARCHAR(32) NOT NULL,
          entitlement_version BIGINT NOT NULL DEFAULT 0,
          access_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
          snapshot_hash VARCHAR(128) NULL,
          freshness_lease_until DATETIME NULL,
          valid_until DATETIME NULL,
          received_at DATETIME NOT NULL,
          disabled BOOLEAN NOT NULL DEFAULT FALSE
        )
    """)
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_user_feature_entitlement (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          user_id VARCHAR(64) NOT NULL,
          feature_key VARCHAR(160) NOT NULL,
          value_type VARCHAR(16) NOT NULL,
          boolean_value BOOLEAN NOT NULL DEFAULT FALSE,
          quota_limit INT NULL,
          period VARCHAR(32) NULL,
          valid_until DATETIME NULL,
          enabled BOOLEAN NOT NULL DEFAULT TRUE,
          entitlement_version BIGINT NOT NULL DEFAULT 0,
          CONSTRAINT uq_recommendation_feature_user_key UNIQUE (user_id, feature_key)
        )
    """)
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_entitlement_event_inbox (
          event_id VARCHAR(128) NOT NULL PRIMARY KEY,
          user_id VARCHAR(64) NOT NULL,
          entitlement_version BIGINT NOT NULL,
          received_at DATETIME NOT NULL
        )
    """)
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_quota_period_counter (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          user_id VARCHAR(64) NOT NULL,
          feature_key VARCHAR(160) NOT NULL,
          period_key VARCHAR(64) NOT NULL,
          period_start DATETIME NOT NULL,
          period_end DATETIME NOT NULL,
          quota_limit INT NOT NULL,
          reserved INT NOT NULL DEFAULT 0,
          committed INT NOT NULL DEFAULT 0,
          version BIGINT NOT NULL DEFAULT 0,
          updated_at DATETIME NOT NULL,
          CONSTRAINT uq_recommendation_quota_period UNIQUE (user_id, feature_key, period_key)
        )
    """)
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_quota_reservation (
          id VARCHAR(64) NOT NULL PRIMARY KEY,
          user_id VARCHAR(64) NOT NULL,
          feature_key VARCHAR(160) NOT NULL,
          period_key VARCHAR(64) NOT NULL,
          idempotency_key VARCHAR(255) NOT NULL,
          fingerprint VARCHAR(128) NOT NULL,
          amount INT NOT NULL,
          status VARCHAR(16) NOT NULL,
          expires_at DATETIME NOT NULL,
          created_at DATETIME NOT NULL,
          settled_at DATETIME NULL,
          CONSTRAINT uq_recommendation_quota_idempotency UNIQUE (user_id, feature_key, idempotency_key)
        )
    """)
    op.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_quota_ledger (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          reservation_id VARCHAR(64) NOT NULL,
          user_id VARCHAR(64) NOT NULL,
          feature_key VARCHAR(160) NOT NULL,
          amount INT NOT NULL,
          entry_type VARCHAR(16) NOT NULL,
          created_at DATETIME NOT NULL
        )
    """)
    # MySQL 8.0 has no ADD COLUMN IF NOT EXISTS; check the live schema instead,
    # so the revision also stays a no-op where these columns already exist.
    _add_column_if_missing("courses", sa.Column("access_model", sa.String(32), nullable=True))
    _add_column_if_missing("courses", sa.Column("subscription_feature_key", sa.String(160), nullable=True))
    _add_column_if_missing(
        "recommendation_user_entitlement_projection",
        sa.Column("access_status", sa.String(32), nullable=False, server_default="UNKNOWN"),
    )


def _add_column_if_missing(table: str, column: sa.Column) -> None:
    columns = {existing["name"] for existing in sa.inspect(op.get_bind()).get_columns(table)}
    if column.name not in columns:
        op.add_column(table, column)


def downgrade():
    # Projection/quota data is durable product state.  Destructive rollback is
    # intentionally disabled; use an audited data-retention operation instead.
    pass
