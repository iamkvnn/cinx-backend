from contextlib import asynccontextmanager
import asyncio
import logging

from fastapi import FastAPI, Response
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from app.core.config import settings
from app.core.logging import configure_logging
from app.messaging.rabbitmq import is_consuming, start_consumer
from app.services.asr import preload_whisper_model
from app.services.job_journal import sweep_expired_jobs


logger = logging.getLogger(__name__)


def log_task_result(task: asyncio.Task) -> None:
    if task.cancelled():
        return
    try:
        task.result()
    except Exception:
        logger.exception("Subtitle AI RabbitMQ consumer stopped unexpectedly")


def log_preload_result(task: asyncio.Task) -> None:
    if task.cancelled():
        return
    try:
        task.result()
        logger.info("Whisper model preloaded")
    except Exception:
        logger.exception("Whisper model preload failed; the first job will load it")


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    logger.info("Starting subtitle AI service")
    await asyncio.to_thread(sweep_expired_jobs)
    if settings.WHISPER_PRELOAD:
        # Kept on app.state so the task is not garbage-collected while it runs.
        app.state.preload_task = asyncio.create_task(asyncio.to_thread(preload_whisper_model))
        app.state.preload_task.add_done_callback(log_preload_result)
    consumer_task = asyncio.create_task(start_consumer())
    consumer_task.add_done_callback(log_task_result)
    yield
    consumer_task.cancel()


app = FastAPI(title=settings.APP_NAME, lifespan=lifespan)


@app.get("/health")
async def health(response: Response) -> dict[str, str]:
    if not is_consuming():
        response.status_code = 503
        return {"status": "DOWN"}
    return {"status": "UP"}


@app.get("/metrics", include_in_schema=False)
def metrics() -> Response:
    # Scraped by Prometheus inside the network; nothing routes it from outside.
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)
