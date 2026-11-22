from datetime import date, datetime

from sqlalchemy import Boolean, Date, DateTime, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class CapabilityState(Base):
    """Kill switch override; a capability with no row follows its registry default."""

    __tablename__ = "capability_state"

    capability_id: Mapped[str] = mapped_column(String(80), primary_key=True)
    enabled: Mapped[bool] = mapped_column(Boolean)
    updated_by: Mapped[str] = mapped_column(String(64))
    reason: Mapped[str | None] = mapped_column(String(500), nullable=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class CapabilityUsage(Base):
    __tablename__ = "capability_usage"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    capability_id: Mapped[str] = mapped_column(String(80), primary_key=True)
    usage_day: Mapped[date] = mapped_column(Date, primary_key=True)
    turns: Mapped[int] = mapped_column(Integer, default=0)


class AiPolicyAcceptance(Base):
    __tablename__ = "ai_policy_acceptance"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    policy_version: Mapped[int] = mapped_column(Integer, primary_key=True)
    accepted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
