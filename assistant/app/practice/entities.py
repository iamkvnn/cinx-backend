from datetime import datetime

from sqlalchemy import DateTime, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base, JsonDoc


class PracticeJob(Base):
    """Practice questions generated for one quiz; each lands as a quiz.add_question the instructor approves."""

    __tablename__ = "practice_job"
    __table_args__ = (Index("ix_practice_job_user_course", "user_id", "course_id"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    course_id: Mapped[str] = mapped_column(String(64))
    quiz_lesson_id: Mapped[str] = mapped_column(String(64))
    # the questions are pending actions of this session, so the review queue lists them by session
    session_id: Mapped[str] = mapped_column(String(36))
    topic: Mapped[str | None] = mapped_column(String(300), nullable=True)
    requested: Mapped[int] = mapped_column(Integer)
    produced: Mapped[int] = mapped_column(Integer, default=0)
    # QUEUED, RUNNING, DONE, or STOPPED (no credits, no material left) and FAILED
    status: Mapped[str] = mapped_column(String(20))
    error: Mapped[str | None] = mapped_column(Text, nullable=True)
    # the quiz's questionText when the job was created; null on jobs made before it was kept
    existing_questions: Mapped[list | None] = mapped_column(JsonDoc, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
