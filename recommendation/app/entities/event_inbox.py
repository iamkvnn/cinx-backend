from datetime import datetime

from sqlalchemy import Index, String
from sqlalchemy.dialects.mysql import DATETIME
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class EventInbox(Base):
    """Events already applied to a projection; a repeated event id is a successful no-op."""

    __tablename__ = "recommendation_event_inbox"

    event_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    routing_key: Mapped[str] = mapped_column(String(160), nullable=False)
    event_type: Mapped[str | None] = mapped_column(String(100), nullable=True)
    aggregate_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    processed_at: Mapped[datetime] = mapped_column(DATETIME(fsp=6), nullable=False, default=datetime.utcnow)

    __table_args__ = (Index("idx_event_inbox_processed", "processed_at"),)
