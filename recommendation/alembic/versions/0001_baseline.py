"""Record the pre-Alembic recommendation schema baseline.

The first tables predate Alembic. An installation that already has them keeps them as they are; an empty database
gets them here, in their pre-Alembic shape, so later revisions apply the same way on both.
"""

import sqlalchemy as sa
from alembic import op

revision = "0001_baseline"
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    existing = set(sa.inspect(op.get_bind()).get_table_names())

    if "courses" not in existing:
        # access_model and subscription_feature_key arrive in 0002
        op.create_table(
            "courses",
            sa.Column("id", sa.String(50), primary_key=True),
            sa.Column("title", sa.String(255), nullable=False),
            sa.Column("description", sa.Text(), nullable=True),
            sa.Column("category_id", sa.String(50), nullable=True),
            sa.Column("category_name", sa.String(100), nullable=True),
            sa.Column("instructor_id", sa.String(50), nullable=True),
            sa.Column("instructor_name", sa.String(255), nullable=True),
            sa.Column("rating", sa.Float(), nullable=True),
            sa.Column("enrollment_count", sa.Integer(), nullable=False, server_default="0"),
            sa.Column("price", sa.Float(), nullable=True),
            sa.Column("discounted_price", sa.Float(), nullable=True),
            sa.Column("discount_rate", sa.Integer(), nullable=True),
            sa.Column("duration", sa.Integer(), nullable=True),
            sa.Column("is_in_subscription", sa.Boolean(), nullable=False, server_default=sa.false()),
            sa.Column("has_certificate", sa.Boolean(), nullable=False, server_default=sa.false()),
            sa.Column("certificate_title", sa.String(255), nullable=True),
            sa.Column("image_urls", sa.JSON(), nullable=True),
            sa.Column("status", sa.String(50), nullable=False, server_default="DRAFT"),
            sa.Column("publish_status", sa.String(50), nullable=True),
            sa.Column("curriculum", sa.JSON(), nullable=True),
            sa.Column("created_at", sa.DateTime(), nullable=True),
            sa.Column("updated_at", sa.DateTime(), nullable=True),
        )
        for name, column in (("idx_course_category_id", "category_id"), ("idx_course_status", "status"),
                             ("idx_course_rating", "rating"), ("idx_course_enrollment", "enrollment_count"),
                             ("idx_course_price", "price"), ("idx_course_certificate", "has_certificate")):
            op.create_index(name, "courses", [column])

    if "course_chunks" not in existing:
        op.create_table(
            "course_chunks",
            sa.Column("id", sa.String(50), primary_key=True),
            sa.Column("course_id", sa.String(50), nullable=False),
            sa.Column("section_title", sa.String(255), nullable=True),
            sa.Column("lesson_ids", sa.JSON(), nullable=True),
            sa.Column("content", sa.Text(), nullable=False),
            sa.Column("embedding", sa.JSON(), nullable=False),
        )

    if "knowledge_documents" not in existing:
        op.create_table(
            "knowledge_documents",
            sa.Column("id", sa.String(50), primary_key=True),
            sa.Column("title", sa.String(255), nullable=False),
            sa.Column("source_type", sa.String(50), nullable=False, server_default="CMS"),
            sa.Column("source_url", sa.String(500), nullable=True),
            sa.Column("content", sa.Text(), nullable=False),
            sa.Column("created_at", sa.DateTime(), nullable=True),
            sa.Column("updated_at", sa.DateTime(), nullable=True),
        )
        op.create_index("idx_knowledge_source_type", "knowledge_documents", ["source_type"])

    if "knowledge_chunks" not in existing:
        op.create_table(
            "knowledge_chunks",
            sa.Column("id", sa.String(50), primary_key=True),
            sa.Column("document_id", sa.String(50), sa.ForeignKey("knowledge_documents.id"), nullable=False),
            sa.Column("content", sa.Text(), nullable=False),
            sa.Column("embedding", sa.JSON(), nullable=False),
            sa.Column("metadata", sa.JSON(), nullable=True),
        )
        op.create_index("idx_knowledge_chunk_document", "knowledge_chunks", ["document_id"])

    if "user_preferences" not in existing:
        op.create_table(
            "user_preferences",
            sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
            sa.Column("user_id", sa.String(50), nullable=False),
            sa.Column("categoryId", sa.String(100), nullable=False),
            sa.Column("created_at", sa.DateTime(), nullable=True),
        )
        op.create_index("idx_user_pref_user", "user_preferences", ["user_id"])
        op.create_index("idx_user_pref_category", "user_preferences", ["categoryId"])
        op.create_index("uq_user_pref_user_category", "user_preferences", ["user_id", "categoryId"], unique=True)


def downgrade():
    # Course projections and knowledge are durable state; destructive rollback is intentionally disabled, as in 0002.
    pass
