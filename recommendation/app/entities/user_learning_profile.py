from datetime import datetime

from sqlalchemy import Boolean, DateTime, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class UserLearningProfile(Base):
    """The part of the user service's learning profile recommendation acts on: the goal and the consent."""

    __tablename__ = "user_learning_profile"

    user_id: Mapped[str] = mapped_column(String(255), primary_key=True)
    goal: Mapped[str | None] = mapped_column(String(30), nullable=True)
    personalization_consent: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    # the snapshot's occurredAt, so an older event redelivered late is ignored
    updated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
