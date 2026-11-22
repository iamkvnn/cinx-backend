import asyncio
import json
import logging
from datetime import datetime, timezone

import aio_pika
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.config import settings
from app.messaging.entities import OutboxEvent


logger = logging.getLogger(__name__)
MAX_ATTEMPTS = 20


def amqp_message(event: OutboxEvent) -> aio_pika.Message:
    headers = {
        "eventId": event.id,
        "eventType": event.event_type,
        "eventVersion": event.event_version,
        "aggregateType": event.aggregate_type,
        "aggregateId": event.aggregate_id,
        "occurredAt": event.occurred_at.isoformat(),
    }
    if event.trace_id:
        headers["traceparent"] = f"00-{event.trace_id}-{event.id.replace('-', '')[:16]}-01"
    return aio_pika.Message(
        body=json.dumps(event.payload, ensure_ascii=False).encode(),
        content_type="application/json",
        message_id=event.id,
        type=event.event_type,
        headers=headers,
        delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
    )


class OutboxRelay:
    """Publishes committed outbox rows with publisher confirms; at-least-once, consumers dedupe on eventId."""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]):
        self.session_factory = session_factory

    async def run(self) -> None:
        while True:
            try:
                connection = await aio_pika.connect_robust(
                    host=settings.RABBITMQ_HOST, port=settings.RABBITMQ_PORT,
                    login=settings.RABBITMQ_USER, password=settings.RABBITMQ_PASSWORD,
                )
                async with connection:
                    channel = await connection.channel(publisher_confirms=True)
                    exchange = await channel.declare_exchange(
                        settings.DOMAIN_EVENTS_EXCHANGE, aio_pika.ExchangeType.TOPIC, durable=True
                    )
                    while True:
                        published = await self.publish_batch(exchange)
                        if published < settings.OUTBOX_RELAY_BATCH_SIZE:
                            await asyncio.sleep(settings.OUTBOX_RELAY_INTERVAL_SECONDS)
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception("Outbox relay lost its broker connection; retrying")
                await asyncio.sleep(5)

    async def publish_batch(self, exchange) -> int:
        async with self.session_factory() as db:
            rows = (
                await db.scalars(
                    select(OutboxEvent)
                    .where(OutboxEvent.published_at.is_(None), OutboxEvent.attempts < MAX_ATTEMPTS)
                    .order_by(OutboxEvent.occurred_at)
                    .limit(settings.OUTBOX_RELAY_BATCH_SIZE)
                    .with_for_update(skip_locked=True)
                )
            ).all()
            for row in rows:
                try:
                    await exchange.publish(amqp_message(row), routing_key=row.routing_key)
                    row.published_at = datetime.now(timezone.utc)
                except Exception as exc:
                    row.attempts += 1
                    row.last_error = str(exc)[:1000]
            await db.commit()
            return len(rows)
