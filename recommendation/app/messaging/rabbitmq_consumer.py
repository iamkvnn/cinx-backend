import aio_pika
import asyncio
import logging
from dataclasses import dataclass

from app.core.logging import reset_context, set_context, trace_headers_from_values
from app.core.config import settings
from app.core.metrics import EVENTS
from app.messaging.event_handlers import event_identity, process_message

logger = logging.getLogger(__name__)

# Delays before each retry of a transient failure; after the last one the
# message goes to the queue's error queue.
RETRY_DELAYS_SECONDS = (5, 30, 120)
RETRY_COUNT_HEADER = "x-retry-count"
ORIGINAL_ROUTING_KEY_HEADER = "x-original-routing-key"
# A message that cannot be read or fails validation will not improve with time.
# pydantic's ValidationError, JSONDecodeError and UnicodeDecodeError are ValueErrors.
PERMANENT_ERRORS = (ValueError, KeyError, TypeError)
# Pause before putting back a message neither queue could take, so a broker
# outage does not spin.
REQUEUE_PAUSE_SECONDS = 1


@dataclass(frozen=True)
class ConsumedQueue:
    queue: aio_pika.abc.AbstractQueue
    retry_queue: str
    error_queue: str


async def start_consumer():
    logger.info("Starting RabbitMQ consumer - host=%s port=%s", settings.RABBITMQ_HOST, settings.RABBITMQ_PORT)
    connection = await aio_pika.connect_robust(
        host=settings.RABBITMQ_HOST,
        port=settings.RABBITMQ_PORT,
        login=settings.RABBITMQ_USER,
        password=settings.RABBITMQ_PASSWORD,
    )

    channel = await connection.channel()
    await channel.set_qos(prefetch_count=50)

    # Commerce entitlement snapshots are full, versioned projections.  They keep
    # a separate queue so a recommendation outage cannot block course ingestion.
    lanes = [
        (settings.RABBITMQ_EXCHANGE, settings.RABBITMQ_QUEUE, f"{settings.RABBITMQ_QUEUE}.error", settings.RABBITMQ_ROUTING_KEYS),
        (settings.ENROLLMENT_EXCHANGE, settings.ENROLLMENT_QUEUE, f"{settings.ENROLLMENT_QUEUE}.error", settings.ENROLLMENT_ROUTING_KEYS),
        (settings.SOCIAL_EXCHANGE, settings.SOCIAL_QUEUE, f"{settings.SOCIAL_QUEUE}.error", settings.SOCIAL_ROUTING_KEYS),
        (settings.USER_EXCHANGE, settings.USER_QUEUE, f"{settings.USER_QUEUE}.error", settings.USER_ROUTING_KEYS),
        (settings.ENTITLEMENT_EXCHANGE, settings.ENTITLEMENT_QUEUE, settings.ENTITLEMENT_DLQ, settings.ENTITLEMENT_ROUTING_KEYS),
    ]
    consumed: list[ConsumedQueue] = []
    for exchange_name, queue_name, error_queue_name, routing_keys in lanes:
        exchange = await channel.declare_exchange(exchange_name, aio_pika.ExchangeType.TOPIC, durable=True)
        queue = await channel.declare_queue(queue_name, durable=True)
        await channel.declare_queue(error_queue_name, durable=True)
        # A retried message waits here for its per-message TTL, then dead-letters
        # back to the queue it came from.
        retry_queue = await channel.declare_queue(
            f"{queue_name}.retry",
            durable=True,
            arguments={"x-dead-letter-exchange": "", "x-dead-letter-routing-key": queue_name},
        )
        for routing_key in [key.strip() for key in routing_keys.split(",") if key.strip()]:
            await queue.bind(exchange, routing_key)
            logger.info("Bound recommendation queue=%s routing_key=%s", queue_name, routing_key)
        consumed.append(ConsumedQueue(queue, retry_queue.name, error_queue_name))

    await asyncio.gather(*(consume_queue(channel, lane) for lane in consumed))


async def consume_queue(channel, lane: ConsumedQueue) -> None:
    async with lane.queue.iterator() as queue_iter:
        async for message in queue_iter:
            await handle_delivery(channel, lane, message)


async def handle_delivery(channel, lane: ConsumedQueue, message) -> None:
    """Ack once the event is applied, parked for a retry, or in the error queue.

    If neither queue takes the message, it goes back on this queue rather than
    being dropped.  A cancelled handler (shutdown) settles nothing, so closing
    the connection returns the message.
    """
    headers = dict(message.headers or {})
    # A retried message arrives through the default exchange, under the queue's name.
    original = headers.get(ORIGINAL_ROUTING_KEY_HEADER)
    if isinstance(original, bytes):
        original = original.decode("utf-8", errors="replace")
    routing_key = str(original or message.routing_key or "")
    retry_count = _int(headers.get(RETRY_COUNT_HEADER))
    tokens = set_context(
        trace_headers_from_values(
            str(headers.get("traceparent")) if headers.get("traceparent") else None,
            str(headers.get("X-Request-Id")) if headers.get("X-Request-Id") else None,
        )
    )
    try:
        try:
            outcome = await process_message(message.body, routing_key, headers, message.message_id)
        except PERMANENT_ERRORS as exc:
            _log_failure("Permanent failure; sending to the error queue", lane, routing_key, message, retry_count, exc)
            await _publish_copy(channel, message, lane.error_queue, _failure_headers(headers, lane, routing_key, retry_count, exc))
            outcome = "dead_lettered"
        except Exception as exc:
            if retry_count < len(RETRY_DELAYS_SECONDS):
                _log_failure("Transient failure; retrying later", lane, routing_key, message, retry_count, exc)
                retry_headers = {
                    **headers,
                    RETRY_COUNT_HEADER: retry_count + 1,
                    ORIGINAL_ROUTING_KEY_HEADER: routing_key,
                    "lastFailureReason": type(exc).__name__,
                }
                await _publish_copy(
                    channel,
                    message,
                    lane.retry_queue,
                    retry_headers,
                    expiration=RETRY_DELAYS_SECONDS[retry_count],
                )
                outcome = "retried"
            else:
                _log_failure("Retries exhausted; sending to the error queue", lane, routing_key, message, retry_count, exc)
                await _publish_copy(channel, message, lane.error_queue, _failure_headers(headers, lane, routing_key, retry_count, exc))
                outcome = "dead_lettered"
        await message.ack()
        EVENTS.labels(lane.queue.name, outcome).inc()
    except Exception:
        logger.exception(
            "Could not settle RabbitMQ message; returning it to the queue - queue=%s routing_key=%s",
            lane.queue.name,
            routing_key,
        )
        await asyncio.sleep(REQUEUE_PAUSE_SECONDS)
        try:
            await message.nack(requeue=True)
        except Exception:
            logger.warning("RabbitMQ channel closed; the message will be redelivered", exc_info=True)
    finally:
        reset_context(tokens)


async def _publish_copy(channel, message, queue_name: str, headers: dict, expiration: float | None = None) -> None:
    await channel.default_exchange.publish(
        aio_pika.Message(
            body=message.body,
            content_type=message.content_type,
            message_id=message.message_id,
            headers=headers,
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            expiration=expiration,
        ),
        routing_key=queue_name,
    )


def _failure_headers(headers: dict, lane: ConsumedQueue, routing_key: str, retry_count: int, exc: Exception) -> dict:
    return {
        **headers,
        "failedRoutingKey": routing_key,
        "failedQueue": lane.queue.name,
        # The class only: an exception message can quote the payload.
        "failureReason": type(exc).__name__,
        "retryCount": retry_count,
    }


def _log_failure(summary: str, lane: ConsumedQueue, routing_key: str, message, retry_count: int, exc: Exception) -> None:
    identity = event_identity(routing_key, message.body, dict(message.headers or {}), message.message_id)
    logger.warning(
        "%s - queue=%s routing_key=%s event_id=%s retry_count=%s error=%s",
        summary,
        lane.queue.name,
        routing_key,
        identity.event_id,
        retry_count,
        type(exc).__name__,
        # A validation error's text quotes the payload, so only other failures get a traceback.
        exc_info=not isinstance(exc, PERMANENT_ERRORS),
    )


def _int(value) -> int:
    try:
        return max(0, int(value))
    except (TypeError, ValueError):
        return 0
