"""Persist explicit subscription-period boundaries from entitlement snapshots."""

import sqlalchemy as sa
from alembic import op

revision = "0003_feature_period_boundaries"
down_revision = "0002_entitlement_quota"
branch_labels = None
depends_on = None


def upgrade():
    # MySQL 8.0 has no ADD COLUMN IF NOT EXISTS; check the live schema instead,
    # so the revision also stays a no-op where these columns already exist.
    columns = {
        column["name"]
        for column in sa.inspect(op.get_bind()).get_columns("recommendation_user_feature_entitlement")
    }
    if "period_start" not in columns:
        op.add_column("recommendation_user_feature_entitlement", sa.Column("period_start", sa.DateTime(), nullable=True))
    if "period_end" not in columns:
        op.add_column("recommendation_user_feature_entitlement", sa.Column("period_end", sa.DateTime(), nullable=True))


def downgrade():
    # Period metadata is part of the entitlement projection and is retained
    # on rollback; use a forward migration for an approved cleanup.
    pass
