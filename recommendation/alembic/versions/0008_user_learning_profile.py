"""The learner's goal and personalization consent, projected from user.learning-profile.updated (P1-06)."""

import sqlalchemy as sa
from alembic import op

revision = "0008_user_learning_profile"
down_revision = "0007_agent_session_meta"
branch_labels = None
depends_on = None


def upgrade():
    if "user_learning_profile" in sa.inspect(op.get_bind()).get_table_names():
        return
    op.create_table(
        "user_learning_profile",
        sa.Column("user_id", sa.String(255), primary_key=True),
        sa.Column("goal", sa.String(30), nullable=True),
        sa.Column("personalization_consent", sa.Boolean(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
    )


def downgrade():
    # A projection of another service's data: the next snapshot rebuilds it, so it may be dropped.
    op.drop_table("user_learning_profile")
