"""Debugging artifacts of a job, written under its work directory.

A failed write is logged and never fails the job.
"""

import json
import logging
from pathlib import Path

from app.models import to_jsonable


logger = logging.getLogger(__name__)


def write_json_artifact(directory: Path | None, file_name: str, value) -> None:
    if directory is None:
        return
    try:
        directory.mkdir(parents=True, exist_ok=True)
        (directory / file_name).write_text(
            json.dumps(to_jsonable(value), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    except Exception:
        logger.warning("Failed to write JSON artifact file=%s", file_name, exc_info=True)


def write_text_artifact(directory: Path | None, file_name: str, content: str) -> None:
    if directory is None:
        return
    try:
        directory.mkdir(parents=True, exist_ok=True)
        (directory / file_name).write_text(content, encoding="utf-8")
    except Exception:
        logger.warning("Failed to write text artifact file=%s", file_name, exc_info=True)
