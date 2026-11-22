import json
import logging
import asyncio
from datetime import datetime, timezone
from uuid import uuid4

import aio_pika
import aiormq

from app.core.config import settings
from app.core.metrics import MESSAGES_REJECTED
from app.core.logging import outgoing_trace_headers, reset_context, set_context, trace_headers_from_values
from app.services.processor import FAILED_ROUTING_KEY, SubtitleJobProcessor, failed_payload


logger = logging.getLogger(__name__)

GENERATE_ROUTING_KEY = "learning.subtitle.generate.requested"
TRANSLATE_ROUTING_KEY = "learning.subtitle.translate.requested"
DEAD_LETTER_ROUTING_KEY = "subtitle-ai.learning.dead"
PUBLISH_RETRY_ATTEMPTS = 3
PUBLISH_RETRY_DELAY_SECONDS = 5
RECONNECT_INITIAL_DELAY_SECONDS = 5
RECONNECT_MAX_DELAY_SECONDS = 60
AMQP_ERRORS = (
    aiormq.exceptions.AMQPError,
    aio_pika.exceptions.AMQPException,
    ConnectionError,
    OSError,
)
_consumer_state = {"consuming": False}


class RejectMessage(Exception):
    """The request goes to the dead-letter queue instead of being acknowledged."""

    def __init__(self, reason: str, detail: str):
        super().__init__(detail)
        # A fixed code (unidentifiable, publish_failed), used as a metric label.
        self.reason = reason


def is_consuming() -> bool:
    return _consumer_state["consuming"]


class SubtitleEventPublisher:
    def __init__(self, exchange: aio_pika.Exchange):
        self.exchange = exchange

    async def publish_progress(self, job_id: str, progress_percent: int) -> None:
        logger.info("Publishing subtitle progress job_id=%s progress=%s", job_id, progress_percent)
        try:
            await self._publish(
                "ai.subtitle.job.progress",
                {"jobId": job_id, "progressPercent": max(0, min(99, int(progress_percent)))},
            )
        except AMQP_ERRORS:
            # Progress is best effort; the job goes on and its result is still published.
            logger.warning("Skipped subtitle progress job_id=%s progress=%s", job_id, progress_percent)

    async def publish_result(self, result: dict) -> None:
        payload = result["payload"]
        logger.info(
            "Publishing subtitle result job_id=%s routing_key=%s event_id=%s",
            payload.get("jobId"),
            result["routingKey"],
            result.get("eventId"),
        )
        await self._publish(result["routingKey"], payload, result.get("eventId"))

    async def publish_failed(self, job_id: str, error_code: str, error_message: str) -> None:
        logger.info("Publishing subtitle failed job_id=%s error_code=%s", job_id, error_code)
        await self._publish(FAILED_ROUTING_KEY, failed_payload(job_id, error_code, error_message))

    async def _publish(self, routing_key: str, payload: dict, event_id: str | None = None) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        event_id = event_id or str(uuid4())
        aggregate_id = str(payload.get("jobId") or event_id)
        occurred_at = datetime.now(timezone.utc).isoformat()
        headers = {
            "eventId": event_id,
            "eventType": event_type(routing_key),
            "eventVersion": 1,
            "aggregateType": "SubtitleJob",
            "aggregateId": aggregate_id,
            "occurredAt": occurred_at,
            **outgoing_trace_headers(),
        }
        exchange_name = getattr(self.exchange, "name", settings.AI_SUBTITLE_EXCHANGE)
        for attempt in range(1, PUBLISH_RETRY_ATTEMPTS + 1):
            try:
                logger.info("Publishing subtitle event exchange=%s routing_key=%s", exchange_name, routing_key)
                await self.exchange.publish(
                    aio_pika.Message(
                        body=body,
                        content_type="application/json",
                        delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                        message_id=event_id,
                        timestamp=datetime.now(timezone.utc),
                        headers=headers,
                    ),
                    routing_key=routing_key,
                    mandatory=True,
                )
                return
            except AMQP_ERRORS:
                if attempt == PUBLISH_RETRY_ATTEMPTS:
                    logger.exception(
                        "Failed to publish subtitle event exchange=%s routing_key=%s after retries",
                        exchange_name,
                        routing_key,
                    )
                    raise
                logger.warning(
                    "RabbitMQ publish failed exchange=%s routing_key=%s attempt=%s/%s; retrying",
                    exchange_name,
                    routing_key,
                    attempt,
                    PUBLISH_RETRY_ATTEMPTS,
                    exc_info=True,
                )
                await asyncio.sleep(PUBLISH_RETRY_DELAY_SECONDS)


def event_type(routing_key: str) -> str:
    return {
        "ai.subtitle.job.progress": "SubtitleJobProgress",
        "ai.subtitle.job.completed": "SubtitleJobCompleted",
        "ai.subtitle.job.failed": "SubtitleJobFailed",
    }.get(routing_key, "SubtitleJobEvent")


async def start_consumer() -> None:
    """Consume until cancelled, reconnecting with backoff when the broker is unreachable."""
    processor = SubtitleJobProcessor()
    delay = RECONNECT_INITIAL_DELAY_SECONDS
    while True:
        try:
            await consume(processor)
            logger.warning("Subtitle AI consumer stopped")
        except asyncio.CancelledError:
            _consumer_state["consuming"] = False
            raise
        except Exception:
            logger.exception("Subtitle AI consumer failed")
        if _consumer_state["consuming"]:
            delay = RECONNECT_INITIAL_DELAY_SECONDS
        _consumer_state["consuming"] = False
        logger.info("Reconnecting subtitle AI consumer in %ss", delay)
        await asyncio.sleep(delay)
        delay = min(delay * 2, RECONNECT_MAX_DELAY_SECONDS)


async def consume(processor: SubtitleJobProcessor) -> None:
    logger.info("Starting subtitle AI RabbitMQ consumer - host=%s port=%s", settings.RABBITMQ_HOST, settings.RABBITMQ_PORT)
    connection = await aio_pika.connect_robust(
        host=settings.RABBITMQ_HOST,
        port=settings.RABBITMQ_PORT,
        login=settings.RABBITMQ_USER,
        password=settings.RABBITMQ_PASSWORD,
    )
    try:
        publisher_channel = await connection.channel()
        ai_exchange = await publisher_channel.declare_exchange(settings.AI_SUBTITLE_EXCHANGE, aio_pika.ExchangeType.TOPIC, durable=True)
        publisher = SubtitleEventPublisher(ai_exchange)

        # One channel per lane, so each has its own prefetch and a long generation
        # never holds back a translation.
        generate_channel = await connection.channel()
        await generate_channel.set_qos(prefetch_count=settings.RABBITMQ_PREFETCH)
        translate_channel = await connection.channel()
        await translate_channel.set_qos(prefetch_count=settings.SUBTITLE_TRANSLATE_PREFETCH)

        learning_exchange = await generate_channel.declare_exchange(settings.LEARNING_EXCHANGE, aio_pika.ExchangeType.TOPIC, durable=True)
        dlx = await generate_channel.declare_exchange(settings.RABBITMQ_DLX, aio_pika.ExchangeType.DIRECT, durable=True)
        dlq = await generate_channel.declare_queue(settings.SUBTITLE_DLQ, durable=True)
        await dlq.bind(dlx, DEAD_LETTER_ROUTING_KEY)

        dead_letter_arguments = {
            "x-dead-letter-exchange": settings.RABBITMQ_DLX,
            "x-dead-letter-routing-key": DEAD_LETTER_ROUTING_KEY,
        }
        generate_queue = await generate_channel.declare_queue(settings.SUBTITLE_QUEUE, durable=True, arguments=dead_letter_arguments)
        translate_queue = await translate_channel.declare_queue(
            settings.SUBTITLE_TRANSLATE_QUEUE, durable=True, arguments=dead_letter_arguments
        )
        await generate_queue.bind(learning_exchange, GENERATE_ROUTING_KEY)
        await translate_queue.bind(learning_exchange, TRANSLATE_ROUTING_KEY)
        logger.info(
            "Bound subtitle AI queues generate=%s translate=%s",
            settings.SUBTITLE_QUEUE,
            settings.SUBTITLE_TRANSLATE_QUEUE,
        )
        # Only after the new lane is bound, so no translation request is ever unroutable.
        await retire_old_translate_binding(connection)

        _consumer_state["consuming"] = True
        async with asyncio.TaskGroup() as lanes:
            lanes.create_task(consume_queue(generate_queue, processor, publisher))
            lanes.create_task(consume_queue(translate_queue, processor, publisher))
    finally:
        try:
            await connection.close()
        except Exception:
            logger.warning("Failed to close the subtitle AI RabbitMQ connection", exc_info=True)


async def consume_queue(queue, processor: SubtitleJobProcessor, publisher: SubtitleEventPublisher) -> None:
    async with queue.iterator() as queue_iter:
        async for message in queue_iter:
            await handle_delivery(message, processor, publisher)


async def retire_old_translate_binding(connection) -> None:
    """Remove the translate binding earlier versions put on the generate queue.

    A separate channel is used because a broker error closes the channel it
    happens on.  Requests already waiting in that queue are still handled.
    """
    channel = await connection.channel()
    try:
        queue = await channel.get_queue(settings.SUBTITLE_QUEUE, ensure=False)
        await queue.unbind(settings.LEARNING_EXCHANGE, TRANSLATE_ROUTING_KEY)
    except AMQP_ERRORS:
        logger.warning("Could not remove the old translate binding queue=%s", settings.SUBTITLE_QUEUE, exc_info=True)
    finally:
        try:
            await channel.close()
        except Exception:
            logger.debug("Temporary RabbitMQ channel already closed", exc_info=True)


async def handle_delivery(message, processor: SubtitleJobProcessor, publisher: SubtitleEventPublisher) -> None:
    """Ack a handled request, reject one that cannot be handled or reported to the DLQ.

    A cancelled job (shutdown) is settled neither way: closing the connection puts the
    request back on the queue, and the job journal resumes it.
    """
    headers = message.headers or {}
    tokens = set_context(
        trace_headers_from_values(
            str(headers.get("traceparent")) if headers.get("traceparent") else None,
            str(headers.get("X-Request-Id")) if headers.get("X-Request-Id") else None,
        )
    )
    try:
        logger.info(
            "Received subtitle AI message routing_key=%s delivery_tag=%s redelivered=%s",
            message.routing_key,
            message.delivery_tag,
            message.redelivered,
        )
        try:
            await handle_message(message.routing_key, message.body, processor, publisher)
        except RejectMessage as exc:
            logger.warning(
                "Rejecting subtitle AI message to the dead-letter queue routing_key=%s reason=%s",
                message.routing_key,
                exc,
            )
            MESSAGES_REJECTED.labels(exc.reason).inc()
            await settle(message.reject(requeue=False))
        except Exception:
            logger.exception("Unexpected error while handling subtitle AI message routing_key=%s", message.routing_key)
            MESSAGES_REJECTED.labels("unexpected").inc()
            await settle(message.reject(requeue=False))
        else:
            await settle(message.ack())
    finally:
        reset_context(tokens)


async def settle(action) -> None:
    try:
        await action
    except Exception:
        logger.warning(
            "RabbitMQ channel closed before the subtitle message was settled; it will be redelivered",
            exc_info=True,
        )


async def handle_message(routing_key: str, body: bytes, processor: SubtitleJobProcessor, publisher: SubtitleEventPublisher) -> None:
    job_id = job_id_from_body(body)
    if not job_id:
        raise RejectMessage("unidentifiable", "Subtitle request has no readable jobId")
    payload = json.loads(body.decode("utf-8"))
    logger.info("Handling subtitle AI request routing_key=%s job_id=%s", routing_key, job_id)
    try:
        await dispatch_message(routing_key, payload, processor, publisher)
    except AMQP_ERRORS as exc:
        logger.exception("Subtitle result event could not be published job_id=%s", job_id)
        raise RejectMessage("publish_failed", "Subtitle result event could not be published") from exc
    except Exception as exc:
        logger.exception("Failed to process subtitle AI message routing_key=%s job_id=%s", routing_key, job_id)
        await publish_failure_from_body(body, publisher, exc)


async def dispatch_message(routing_key: str, payload: dict, processor: SubtitleJobProcessor, publisher: SubtitleEventPublisher) -> None:
    if routing_key == GENERATE_ROUTING_KEY:
        await processor.process_generate(payload, publisher)
    elif routing_key == TRANSLATE_ROUTING_KEY:
        await processor.process_translate(payload, publisher)
    else:
        logger.warning("Ignored unsupported subtitle request routing key: %s", routing_key)


async def publish_failure_from_body(body: bytes, publisher: SubtitleEventPublisher, exc: Exception) -> None:
    """Report a failure to learning; if that is impossible, the request goes to the DLQ."""
    job_id = job_id_from_body(body)
    if not job_id:
        raise RejectMessage("unidentifiable", "Subtitle request has no readable jobId")
    try:
        await publisher.publish_failed(job_id, "AI_SUBTITLE_FAILED", str(exc))
    except AMQP_ERRORS as publish_error:
        logger.exception("Failed to publish subtitle failure event job_id=%s because RabbitMQ is unavailable", job_id)
        raise RejectMessage("publish_failed", "Subtitle failure event could not be published") from publish_error


def job_id_from_body(body: bytes) -> str | None:
    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, ValueError):
        return None
    job_id = payload.get("jobId") if isinstance(payload, dict) else None
    if job_id is None:
        return None
    return str(job_id).strip() or None
