"""Local, consumer-owned entitlement and quota projections.

The recommendation service never treats Commerce as a hot-path dependency.  It
enforces the advanced-agent feature from these projections and reconciles them
from the Commerce snapshot event.
"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Index, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class UserEntitlementProjection(Base):
    __tablename__ = "recommendation_user_entitlement_projection"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    subject_type: Mapped[str] = mapped_column(String(32), nullable=False, default="USER")
    entitlement_version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    access_status: Mapped[str] = mapped_column(String(32), nullable=False, default="UNKNOWN")
    snapshot_hash: Mapped[str | None] = mapped_column(String(128), nullable=True)
    freshness_lease_until: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    valid_until: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    received_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    disabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)


class UserFeatureEntitlement(Base):
    __tablename__ = "recommendation_user_feature_entitlement"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    feature_key: Mapped[str] = mapped_column(String(160), nullable=False)
    value_type: Mapped[str] = mapped_column(String(16), nullable=False, default="BOOLEAN")
    boolean_value: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    quota_limit: Mapped[int | None] = mapped_column(Integer, nullable=True)
    period: Mapped[str | None] = mapped_column(String(32), nullable=True)
    period_start: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    period_end: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    valid_until: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    entitlement_version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)

    __table_args__ = (
        UniqueConstraint("user_id", "feature_key", name="uq_recommendation_feature_user_key"),
        Index("idx_recommendation_feature_user", "user_id"),
    )


class EntitlementEventInbox(Base):
    __tablename__ = "recommendation_entitlement_event_inbox"

    event_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    entitlement_version: Mapped[int] = mapped_column(BigInteger, nullable=False)
    received_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)


class QuotaPeriodCounter(Base):
    __tablename__ = "recommendation_quota_period_counter"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    feature_key: Mapped[str] = mapped_column(String(160), nullable=False)
    period_key: Mapped[str] = mapped_column(String(64), nullable=False)
    period_start: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    period_end: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    quota_limit: Mapped[int] = mapped_column(Integer, nullable=False)
    reserved: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    committed: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    updated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("user_id", "feature_key", "period_key", name="uq_recommendation_quota_period"),
        Index("idx_recommendation_quota_user", "user_id"),
    )


class QuotaReservation(Base):
    __tablename__ = "recommendation_quota_reservation"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    feature_key: Mapped[str] = mapped_column(String(160), nullable=False)
    period_key: Mapped[str] = mapped_column(String(64), nullable=False)
    idempotency_key: Mapped[str] = mapped_column(String(255), nullable=False)
    fingerprint: Mapped[str] = mapped_column(String(128), nullable=False)
    amount: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="RESERVED")
    expires_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    settled_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    __table_args__ = (
        UniqueConstraint("user_id", "feature_key", "idempotency_key", name="uq_recommendation_quota_idempotency"),
        Index("idx_recommendation_quota_reservation_expiry", "status", "expires_at"),
    )


class QuotaLedger(Base):
    __tablename__ = "recommendation_quota_ledger"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    reservation_id: Mapped[str] = mapped_column(String(64), nullable=False)
    user_id: Mapped[str] = mapped_column(String(64), nullable=False)
    feature_key: Mapped[str] = mapped_column(String(160), nullable=False)
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    entry_type: Mapped[str] = mapped_column(String(16), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (Index("idx_recommendation_quota_ledger_reservation", "reservation_id"),)
