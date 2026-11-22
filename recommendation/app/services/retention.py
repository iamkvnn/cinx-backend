import logging
import threading
from datetime import datetime, timedelta

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import SessionLocal
from app.entities.event_inbox import EventInbox
from app.entities.recommendation_slate import RecommendationSlate


logger = logging.getLogger(__name__)

FIRST_RUN_DELAY_SECONDS = 60
INTERVAL_SECONDS = 24 * 3600
BATCH_SIZE = 500


def purge_projection_data(db: Session, now: datetime | None = None) -> dict[str, int]:
    """Delete inbox rows and recommendation slates past their retention; 0 days keeps them."""
    now = now or datetime.utcnow()
    counts = {"inboxEvents": 0, "slates": 0}
    for key, days, entity, key_column, time_column in (
        ("inboxEvents", settings.EVENT_INBOX_RETENTION_DAYS, EventInbox, EventInbox.event_id, EventInbox.processed_at),
        (
            "slates",
            settings.RECOMMENDATION_SLATE_RETENTION_DAYS,
            RecommendationSlate,
            RecommendationSlate.id,
            RecommendationSlate.created_at,
        ),
    ):
        if days <= 0:
            continue
        cutoff = now - timedelta(days=days)
        while True:
            ids = db.execute(select(key_column).where(time_column < cutoff).limit(BATCH_SIZE)).scalars().all()
            if not ids:
                break
            counts[key] += db.execute(delete(entity).where(key_column.in_(ids))).rowcount
            db.commit()
    return counts


class RetentionScheduler:
    """Runs the purge shortly after startup, then once a day, each time with its own session."""

    def __init__(self):
        self._lock = threading.Lock()
        self._timer: threading.Timer | None = None
        self._stopped = False

    def start(self) -> None:
        self._schedule(FIRST_RUN_DELAY_SECONDS)

    def stop(self) -> None:
        with self._lock:
            self._stopped = True
            if self._timer is not None:
                self._timer.cancel()
                self._timer = None

    def _schedule(self, delay_seconds: float) -> None:
        with self._lock:
            if self._stopped:
                return
            self._timer = threading.Timer(delay_seconds, self._run)
            self._timer.daemon = True
            self._timer.start()

    def _run(self) -> None:
        try:
            db = SessionLocal()
            try:
                counts = purge_projection_data(db)
            finally:
                db.close()
            logger.info("Retention purge completed counts=%s", counts)
        except Exception:
            logger.exception("Retention purge failed")
        finally:
            self._schedule(INTERVAL_SECONDS)


retention_scheduler = RetentionScheduler()
