from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class UserEntitlement(Base):
    """Local copy of Commerce's latest entitlement snapshot for one user; Commerce stays the authority."""

    __tablename__ = "user_entitlement"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    entitlement_version: Mapped[int] = mapped_column(BigInteger)
    access_status: Mapped[str] = mapped_column(String(20))
    reconcile_by: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    access_effective_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class UserFeature(Base):
    __tablename__ = "user_feature"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    feature_key: Mapped[str] = mapped_column(String(160), primary_key=True)
    value_type: Mapped[str] = mapped_column(String(16))
    enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    quota_limit: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    unlimited: Mapped[bool] = mapped_column(Boolean, default=False)
    unit: Mapped[str | None] = mapped_column(String(16), nullable=True)
    reset_policy: Mapped[str | None] = mapped_column(String(32), nullable=True)
    period_start: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    period_end: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    effective_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class EntitlementInbox(Base):
    """Snapshot events already applied, so a redelivered eventId is a no-op."""

    __tablename__ = "entitlement_inbox"

    event_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    entitlement_version: Mapped[int] = mapped_column(BigInteger)
    outcome: Mapped[str] = mapped_column(String(16))
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class CreditCounter(Base):
    """Credits used by one user in one quota period, reserved and settled."""

    __tablename__ = "credit_counter"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    feature_key: Mapped[str] = mapped_column(String(160), primary_key=True)
    period_key: Mapped[str] = mapped_column(String(64), primary_key=True)
    used: Mapped[int] = mapped_column(BigInteger, default=0)
    reserved: Mapped[int] = mapped_column(BigInteger, default=0)


class CreditReservation(Base):
    __tablename__ = "credit_reservation"
    __table_args__ = (Index("ix_credit_reservation_status_created", "status", "created_at"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    feature_key: Mapped[str] = mapped_column(String(160))
    period_key: Mapped[str] = mapped_column(String(64))
    amount: Mapped[int] = mapped_column(Integer)
    status: Mapped[str] = mapped_column(String(16))
    charged: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    settled_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    # when the sweeper may free the hold; None keeps the sweeper's default window (a chat turn)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
