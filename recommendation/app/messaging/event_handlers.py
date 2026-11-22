import asyncio
import hashlib
import json
import logging
from dataclasses import dataclass

from sqlalchemy.exc import IntegrityError

from app.core.database import SessionLocal
from app.agent.services.knowledge import KnowledgeService
from app.entities.event_inbox import EventInbox
from app.models.events import (CourseEvent, EnrolledCourseEvent, PolicyKnowledgeEvent, UserLearningProfileEvent,
                               UserPreferenceEvent, WishlistEvent)
from app.services.sync_service import SyncService
from app.services.rag_index import schedule_rag_rebuild
from app.messaging.routing_keys import ENROLLMENT_GRANTED, LEARNING_PROFILE_UPDATED, is_wishlist_event
from app.messaging.routing_keys import COMMERCE_ENTITLEMENTS_CHANGED
from app.services.entitlement_service import apply_snapshot

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class EventIdentity:
    event_id: str
    event_type: str | None
    aggregate_id: str | None


def event_identity(routing_key: str, body: bytes, headers: dict, message_id: str | None) -> EventIdentity:
    """The producer's ``eventId`` header, else the AMQP message id, else a hash of the message."""
    event_id = _header_text(headers.get("eventId"), 128) or _header_text(message_id, 128) or ""
    if not event_id:
        event_id = hashlib.sha256(routing_key.encode("utf-8") + b"\n" + body).hexdigest()
    return EventIdentity(
        event_id=event_id[:128],
        event_type=_header_text(headers.get("eventType"), 100),
        aggregate_id=_header_text(headers.get("aggregateId"), 128),
    )


async def process_message(
    message_body: bytes,
    routing_key: str,
    headers: dict | None = None,
    message_id: str | None = None,
) -> str:
    """Apply one event; returns ``processed``, ``duplicate``, ``stale`` or ``ignored``.

    Handling is synchronous (database, embeddings), so it runs off the event
    loop; to_thread copies the context, so the logging correlation ids follow.
    Exceptions propagate: the consumer decides between retry and the error queue.
    """
    return await asyncio.to_thread(_process_message_sync, message_body, routing_key, headers or {}, message_id)


def _process_message_sync(message_body: bytes, routing_key: str, headers: dict, message_id: str | None) -> str:
    identity = event_identity(routing_key, message_body, headers, message_id)
    # Payloads can carry policy text or personal data; log identities only.
    logger.info(
        "Received RabbitMQ message - routing_key=%s event_id=%s event_type=%s aggregate_id=%s",
        routing_key,
        identity.event_id,
        identity.event_type,
        identity.aggregate_id,
    )
    raw = json.loads(message_body.decode("utf-8"))
    db = SessionLocal()
    rebuild = False
    try:
        if routing_key == COMMERCE_ENTITLEMENTS_CHANGED:
            # Snapshots carry their own inbox and version check.
            result = apply_snapshot(db, raw)
            db.commit()
            logger.info("Processed entitlement snapshot - result=%s event_id=%s", result, identity.event_id)
            return {"applied": "processed"}.get(result, result)

        if db.get(EventInbox, identity.event_id) is not None:
            logger.info("Skipped duplicate event - routing_key=%s event_id=%s", routing_key, identity.event_id)
            return "duplicate"

        outcome, rebuild = _apply(db, raw, routing_key)
        # The inbox row commits with the projection change, or neither does.
        db.add(
            EventInbox(
                event_id=identity.event_id,
                routing_key=routing_key[:160],
                event_type=identity.event_type,
                aggregate_id=identity.aggregate_id,
            )
        )
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            if db.get(EventInbox, identity.event_id) is None:
                raise
            # The same event was applied concurrently; this delivery changes nothing.
            logger.info("Skipped concurrent duplicate event - routing_key=%s event_id=%s", routing_key, identity.event_id)
            return "duplicate"
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()

    if rebuild:
        schedule_rag_rebuild()
    logger.info("Processed event - routing_key=%s event_id=%s outcome=%s", routing_key, identity.event_id, outcome)
    return outcome


def _apply(db, raw: dict, routing_key: str) -> tuple[str, bool]:
    """Apply the event without committing; returns (outcome, index rebuild needed)."""
    sync_service = SyncService(db)

    if routing_key.startswith("course."):
        event = CourseEvent(**raw)
        result = sync_service.handle_course_upsert(event.course)
        if result == "stale":
            return "stale", False
        return "processed", result == "changed"

    if routing_key == ENROLLMENT_GRANTED:
        event = EnrolledCourseEvent(**raw)
        sync_service.handle_enrollment(event.userId, event.courseId)
        return "processed", False

    if is_wishlist_event(routing_key):
        event = WishlistEvent(**raw)
        sync_service.handle_wishlist(event.userId, event.courseId, event.added)
        return "processed", False

    if routing_key == "user.preference.updated":
        event = UserPreferenceEvent(**raw)
        sync_service.handle_user_preferences(event.payload)
        return "processed", False

    if routing_key == LEARNING_PROFILE_UPDATED:
        event = UserLearningProfileEvent(**raw)
        return ("processed" if sync_service.handle_learning_profile(event) else "stale"), False

    if routing_key == "user.policy.published":
        event = PolicyKnowledgeEvent(**raw)
        if not event.title or not event.content:
            raise ValueError("Policy published event requires title and content")
        KnowledgeService(db).replace_document(
            document_id=event.documentId,
            title=event.title,
            content=event.content,
            source_type=event.sourceType,
            source_url=event.sourceUrl,
        )
        return "processed", True

    if routing_key == "user.policy.archived":
        event = PolicyKnowledgeEvent(**raw)
        return "processed", KnowledgeService(db).delete_document(event.documentId)

    logger.warning("Ignored RabbitMQ message with unsupported routing_key=%s", routing_key)
    return "ignored", False


def _header_text(value, limit: int) -> str | None:
    if isinstance(value, bytes):
        value = value.decode("utf-8", errors="replace")
    text = str(value).strip() if value is not None else ""
    return text[:limit] or None
