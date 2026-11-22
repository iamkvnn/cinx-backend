from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base, JsonDoc


class PendingAction(Base):
    """A write the agent proposed. Nothing reaches a domain service until a permitted person approves it."""

    __tablename__ = "pending_action"
    __table_args__ = (
        Index("ix_pending_action_actor_status", "actor_user_id", "status"),
        Index("ix_pending_action_status_expires", "status", "expires_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    type: Mapped[str] = mapped_column(String(80))
    version: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(20))
    actor_user_id: Mapped[str] = mapped_column(String(64))
    persona: Mapped[str] = mapped_column(String(20))
    capability_id: Mapped[str] = mapped_column(String(80))
    session_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    target_service: Mapped[str] = mapped_column(String(40))
    payload: Mapped[dict] = mapped_column(JsonDoc)
    preview: Mapped[dict] = mapped_column(JsonDoc)
    approver_rule: Mapped[str] = mapped_column(String(20))
    approver_permission: Mapped[str | None] = mapped_column(String(120), nullable=True)
    requires_mfa: Mapped[bool] = mapped_column(default=False)
    idempotency_key: Mapped[str] = mapped_column(String(120), unique=True)
    result: Mapped[dict | None] = mapped_column(JsonDoc, nullable=True)
    error_code: Mapped[str | None] = mapped_column(String(80), nullable=True)
    error_detail: Mapped[str | None] = mapped_column(Text, nullable=True)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    decided_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    decided_by: Mapped[str | None] = mapped_column(String(64), nullable=True)
    decision_reason: Mapped[str | None] = mapped_column(Text, nullable=True)
    executed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class ActionRevision(Base):
    """The draft as it was at each version, so the learner can go back to an earlier one."""

    __tablename__ = "action_revision"

    pending_action_id: Mapped[str] = mapped_column(ForeignKey("pending_action.id", ondelete="CASCADE"), primary_key=True)
    version: Mapped[int] = mapped_column(Integer, primary_key=True)
    payload: Mapped[dict] = mapped_column(JsonDoc)
    preview: Mapped[dict] = mapped_column(JsonDoc)
    # ASSISTANT when the agent proposed it, LEARNER for a draft or an edit made by hand
    author: Mapped[str] = mapped_column(String(20))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class ActionAudit(Base):
    """Append-only trail: who proposed, on whose behalf, who decided, what was sent and what came back."""

    __tablename__ = "action_audit"

    id: Mapped[int] = mapped_column(BigInteger().with_variant(Integer, "sqlite"), primary_key=True, autoincrement=True)
    pending_action_id: Mapped[str] = mapped_column(ForeignKey("pending_action.id"), index=True)
    event: Mapped[str] = mapped_column(String(30))
    actor_user_id: Mapped[str] = mapped_column(String(64))
    on_behalf_of: Mapped[str] = mapped_column(String(64))
    payload_hash: Mapped[str] = mapped_column(String(64))
    outcome_code: Mapped[str | None] = mapped_column(String(80), nullable=True)
    detail: Mapped[str | None] = mapped_column(Text, nullable=True)
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
