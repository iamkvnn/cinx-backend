import json
import logging
import re
import shutil
import time
from datetime import datetime, timezone
from pathlib import Path
from uuid import uuid4

from app.core.config import settings


logger = logging.getLogger(__name__)

STATE_FILE = "state.json"
RUNNING = "RUNNING"
COMPLETED = "COMPLETED"
FAILED = "FAILED"
TERMINAL_STATUSES = {COMPLETED, FAILED}
# The downloaded video and the WAV extracted from it both carry this prefix.
MEDIA_PREFIX = "01_downloaded_media"
JOB_ID_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}")


class JobJournal:
    """Per-job state under WORK_DIR, so a redelivered request can be replayed or resumed."""

    def __init__(self, job_id: str, root: Path | None = None):
        if not JOB_ID_RE.fullmatch(job_id):
            raise ValueError("jobId must be a plain identifier")
        self.job_id = job_id
        self.directory = (root or Path(settings.WORK_DIR)) / job_id
        self.path = self.directory / STATE_FILE
        self.state = self._load()

    @property
    def status(self) -> str | None:
        return self.state.get("status")

    @property
    def attempts(self) -> int:
        return int(self.state.get("attempts") or 0)

    @property
    def result(self) -> dict | None:
        result = self.state.get("result")
        return result if isinstance(result, dict) else None

    @property
    def finished(self) -> bool:
        return self.status in TERMINAL_STATUSES and self.result is not None

    def begin_attempt(self) -> int:
        self.state["status"] = RUNNING
        self.state["attempts"] = self.attempts + 1
        self._save()
        return self.attempts

    def finish(self, status: str, routing_key: str, payload: dict) -> dict:
        """Record the result event before it is published; the media is no longer needed."""
        result = {"routingKey": routing_key, "payload": payload, "eventId": str(uuid4())}
        self.state.update(status=status, result=result)
        self._save()
        remove_media(self.directory)
        return result

    def _load(self) -> dict:
        try:
            state = json.loads(self.path.read_text(encoding="utf-8"))
            return state if isinstance(state, dict) else {}
        except FileNotFoundError:
            return {}
        except (OSError, ValueError):
            logger.warning("Unreadable subtitle job journal job_id=%s; starting fresh", self.job_id, exc_info=True)
            return {}

    def _save(self) -> None:
        self.state["updatedAt"] = datetime.now(timezone.utc).isoformat()
        try:
            self.directory.mkdir(parents=True, exist_ok=True)
            temporary = self.path.with_suffix(".tmp")
            temporary.write_text(json.dumps(self.state, ensure_ascii=False), encoding="utf-8")
            temporary.replace(self.path)
        except OSError:
            logger.warning("Failed to write subtitle job journal job_id=%s", self.job_id, exc_info=True)


def remove_media(directory: Path) -> None:
    for path in directory.glob(f"{MEDIA_PREFIX}*"):
        try:
            path.unlink(missing_ok=True)
        except OSError:
            logger.warning("Failed to delete subtitle job media file=%s", path.name, exc_info=True)


def sweep_expired_jobs(keep_job_id: str | None = None) -> None:
    """Delete job directories untouched for ARTIFACT_RETENTION_DAYS; 0 keeps them forever."""
    if settings.ARTIFACT_RETENTION_DAYS <= 0:
        return
    root = Path(settings.WORK_DIR)
    if not root.is_dir():
        return
    cutoff = time.time() - settings.ARTIFACT_RETENTION_DAYS * 86400
    for directory in root.iterdir():
        if not directory.is_dir() or directory.name == keep_job_id:
            continue
        try:
            last_modified = max(path.stat().st_mtime for path in [directory, *directory.iterdir()])
            if last_modified < cutoff:
                shutil.rmtree(directory)
                logger.info("Deleted expired subtitle job artifacts job_id=%s", directory.name)
        except OSError:
            logger.warning("Failed to sweep subtitle job artifacts job_id=%s", directory.name, exc_info=True)
