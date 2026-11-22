from datetime import datetime

from sqlalchemy import Index, String
from sqlalchemy.dialects.mysql import DATETIME, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class RecommendationSlate(Base):
    """One /recommendations/me response, so a later interaction can name where it came from."""

    __tablename__ = "recommendation_slate"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    ranker_version: Mapped[str] = mapped_column(String(64), nullable=False)
    course_ids: Mapped[list] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DATETIME(fsp=6), nullable=False, default=datetime.utcnow)

    __table_args__ = (Index("idx_recommendation_slate_user_created", "user_id", "created_at"),)
