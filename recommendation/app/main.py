from contextlib import asynccontextmanager
from anyio import to_thread
from fastapi import FastAPI
from fastapi import HTTPException
from fastapi import Response
from fastapi.exceptions import RequestValidationError
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
import logging

from app.core.config import settings
from app.core.database import ensure_schema
from app.core.database import SessionLocal
from app.core.logging import CorrelationMiddleware, configure_logging
from app.core.problem import (
    ProblemDetailException,
    http_exception_handler,
    problem_detail_exception_handler,
    validation_exception_handler,
)
from app.api.recommendation import router as recommendation_router
from app.messaging.rabbitmq_consumer import start_consumer
from app.services.rag_index import rebuild_rag_index
from app.services.retention import retention_scheduler


logger = logging.getLogger(__name__)

def log_task_result(task):
    if task.cancelled():
        return
    try:
        task.result()
    except Exception:
        logger.exception("Recommendation RabbitMQ consumer stopped unexpectedly")

@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    logger.info("Starting recommendation service")
    # Each SSE turn holds a worker thread while a model call runs.
    to_thread.current_default_thread_limiter().total_tokens = settings.THREADPOOL_SIZE
    ensure_schema()
    db = SessionLocal()
    try:
        rebuild_rag_index(db)
    finally:
        db.close()

    retention_scheduler.start()

    import asyncio
    consumer_task = asyncio.create_task(start_consumer())
    consumer_task.add_done_callback(log_task_result)
    yield
    consumer_task.cancel()
    retention_scheduler.stop()


app = FastAPI(title=settings.APP_NAME, lifespan=lifespan)
app.add_middleware(CorrelationMiddleware)
app.add_exception_handler(ProblemDetailException, problem_detail_exception_handler)
app.add_exception_handler(HTTPException, http_exception_handler)
app.add_exception_handler(RequestValidationError, validation_exception_handler)
app.include_router(recommendation_router)


@app.get("/metrics", include_in_schema=False)
def metrics() -> Response:
    # Scraped by Prometheus inside the network; the gateway does not route it.
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)
