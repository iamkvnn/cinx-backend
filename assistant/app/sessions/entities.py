from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base, JsonDoc


class AgentSession(Base):
    __tablename__ = "agent_session"
    __table_args__ = (Index("ix_agent_session_user_updated", "user_id", "updated_at"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    persona: Mapped[str] = mapped_column(String(20))
    placement: Mapped[str] = mapped_column(String(40))
    title: Mapped[str | None] = mapped_column(String(200), nullable=True)
    # The next turn starts with this capability loaded; the agent may still switch.
    last_capability_id: Mapped[str | None] = mapped_column(String(80), nullable=True)
    # The earlier turns, folded into a summary the model reads instead; turns after summary_turn_id stay verbatim.
    summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    summary_turn_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    compacted_messages: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class AgentTurn(Base):
    """One user message and everything the agent did for it, replayable by idempotency key."""

    __tablename__ = "agent_turn"
    __table_args__ = (
        UniqueConstraint("session_id", "idempotency_key", name="uq_agent_turn_idempotency"),
        Index("ix_agent_turn_session_created", "session_id", "created_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    session_id: Mapped[str] = mapped_column(ForeignKey("agent_session.id", ondelete="CASCADE"))
    idempotency_key: Mapped[str] = mapped_column(String(120))
    status: Mapped[str] = mapped_column(String(20))
    user_message: Mapped[str] = mapped_column(Text)
    answer: Mapped[str | None] = mapped_column(Text, nullable=True)
    capability_id: Mapped[str | None] = mapped_column(String(80), nullable=True)
    # pydantic-ai ModelMessage list of this turn only; the next turn's history is built from these.
    model_messages: Mapped[list | None] = mapped_column(JsonDoc, nullable=True)
    events: Mapped[list | None] = mapped_column(JsonDoc, nullable=True)
    input_tokens: Mapped[int] = mapped_column(Integer, default=0)
    output_tokens: Mapped[int] = mapped_column(Integer, default=0)
    # One reservation per attempt: a failed turn retried under the same key is held again.
    reservation_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    credits_charged: Mapped[int] = mapped_column(Integer, default=0)
    error_code: Mapped[str | None] = mapped_column(String(80), nullable=True)
    # the tutor mode the learner picked, the model that answered and the page it was asked on, for a later report
    mode: Mapped[str | None] = mapped_column(String(30), nullable=True)
    model: Mapped[str | None] = mapped_column(String(120), nullable=True)
    context: Mapped[dict | None] = mapped_column(JsonDoc, nullable=True)
    # how full the model's window was after this turn: the last request's prompt plus its answer, and the window
    context_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    context_limit: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class AgentTurnEvent(Base):
    """One event of a turn, in the order it was streamed; a client that left resumes after the last seq it saw."""

    __tablename__ = "agent_turn_event"

    turn_id: Mapped[str] = mapped_column(ForeignKey("agent_turn.id", ondelete="CASCADE"), primary_key=True)
    seq: Mapped[int] = mapped_column(Integer, primary_key=True)
    event: Mapped[str] = mapped_column(String(40))
    data: Mapped[dict] = mapped_column(JsonDoc)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class AdminToolAudit(Base):
    """One row per tool an admin's copilot ran: the admin persona reads data an admin sees only with MFA."""

    __tablename__ = "admin_tool_audit"
    __table_args__ = (Index("ix_admin_tool_audit_user_time", "user_id", "occurred_at"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(64))
    session_id: Mapped[str] = mapped_column(String(36))
    capability_id: Mapped[str | None] = mapped_column(String(80), nullable=True)
    tool: Mapped[str] = mapped_column(String(120))
    # sha256 of the tool input: what was asked can be matched without storing it
    input_hash: Mapped[str] = mapped_column(String(64))
    outcome: Mapped[str] = mapped_column(String(20))
    mfa: Mapped[bool] = mapped_column(default=False)
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
