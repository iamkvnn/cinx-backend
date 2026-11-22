from datetime import datetime

from sqlalchemy import DateTime, Index, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class LearnerMemory(Base):
    """One thing the learner approved the assistant to remember about them."""

    __tablename__ = "learner_memory"
    __table_args__ = (Index("ix_learner_memory_user_used", "user_id", "last_used_at"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    kind: Mapped[str] = mapped_column(String(20))
    text: Mapped[str] = mapped_column(String(300))
    # the pending action whose approval saved it, so a retried execution does not save it twice
    source_action_id: Mapped[str | None] = mapped_column(String(36), nullable=True, unique=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    # read into a turn's prompt; twelve months without it and the memory is deleted
    last_used_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class MemoryConsent(Base):
    """Whether the learner lets the assistant propose memories. Withdrawing deletes every memory."""

    __tablename__ = "memory_consent"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    granted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
