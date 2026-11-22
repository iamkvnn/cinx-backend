from sqlalchemy import String, Text, Integer, Float, DateTime, Index, Boolean
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.mysql import JSON
from datetime import datetime
from app.core.database import Base


class Course(Base):
    __tablename__ = "courses"

    id: Mapped[str] = mapped_column(String(50), primary_key=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)

    # Category (stored flat)
    category_id: Mapped[str | None] = mapped_column(String(50), nullable=True)
    category_name: Mapped[str | None] = mapped_column(String(100), nullable=True)

    # Instructor (stored flat)
    instructor_id: Mapped[str | None] = mapped_column(String(50), nullable=True)
    instructor_name: Mapped[str | None] = mapped_column(String(255), nullable=True)

    rating: Mapped[float | None] = mapped_column(Float, nullable=True)
    enrollment_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    price: Mapped[float | None] = mapped_column(Float, nullable=True)
    discounted_price: Mapped[float | None] = mapped_column(Float, nullable=True)
    discount_rate: Mapped[int | None] = mapped_column(Integer, nullable=True)
    duration: Mapped[int | None] = mapped_column(Integer, nullable=True)
    is_in_subscription: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_model: Mapped[str | None] = mapped_column(String(32), nullable=True)
    subscription_feature_key: Mapped[str | None] = mapped_column(String(160), nullable=True)
    has_certificate: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    certificate_title: Mapped[str | None] = mapped_column(String(255), nullable=True)
    image_urls: Mapped[list | None] = mapped_column(JSON, nullable=True)

    status: Mapped[str] = mapped_column(String(50), nullable=False, default="DRAFT")  # "DRAFT", "PUBLISHED", "ARCHIVED"
    publish_status: Mapped[str | None] = mapped_column(String(50), nullable=True)
    curriculum: Mapped[list | None] = mapped_column(JSON, nullable=True)

    created_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    updated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    __table_args__ = (
        Index("idx_course_category_id", "category_id"),
        Index("idx_course_status", "status"),
        Index("idx_course_rating", "rating"),
        Index("idx_course_enrollment", "enrollment_count"),
        Index("idx_course_price", "price"),
        Index("idx_course_certificate", "has_certificate"),
    )
