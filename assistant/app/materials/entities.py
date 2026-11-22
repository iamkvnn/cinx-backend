from datetime import datetime

from sqlalchemy import DateTime, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class CourseMaterial(Base):
    """A file an instructor gave the assistant for one course; its text lives in the knowledge index as MATERIAL."""

    __tablename__ = "course_material"
    __table_args__ = (Index("ix_course_material_course", "course_id"), Index("ix_course_material_status", "status"))

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    course_id: Mapped[str] = mapped_column(String(64))
    uploaded_by: Mapped[str] = mapped_column(String(64))
    # the S3 key learning's course presigned upload made; the file stays there
    file_key: Mapped[str] = mapped_column(String(300))
    file_name: Mapped[str] = mapped_column(String(200))
    content_type: Mapped[str] = mapped_column(String(120))
    # PENDING -> READY or FAILED; extraction runs in the background
    status: Mapped[str] = mapped_column(String(20))
    error: Mapped[str | None] = mapped_column(Text, nullable=True)
    # pages (PDF), slides (PPTX) or paragraphs (DOCX) read
    units: Mapped[int] = mapped_column(Integer, default=0)
    attempts: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
