import uuid
from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.logging import trace_id_var
from app.messaging.entities import OutboxEvent


@dataclass(frozen=True)
class EventKind:
    event_type: str
    routing_key: str
    version: int = 1


ACTION_DECIDED = EventKind("AssistantActionDecided", "assistant.action.decided")


def add_event(db: AsyncSession, kind: EventKind, *, aggregate_type: str, aggregate_id: str, payload: dict) -> OutboxEvent:
    """Stage an event in the caller's transaction; it is published only if that transaction commits."""
    event = OutboxEvent(
        id=str(uuid.uuid4()),
        event_type=kind.event_type,
        event_version=kind.version,
        routing_key=kind.routing_key,
        aggregate_type=aggregate_type,
        aggregate_id=aggregate_id,
        payload=payload,
        trace_id=trace_id_var.get() or None,
        occurred_at=datetime.now(timezone.utc),
    )
    db.add(event)
    return event
