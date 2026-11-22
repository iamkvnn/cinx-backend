"""Dependency-free quota and snapshot primitives used by tests and adapters."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
import hashlib
import json


def request_fingerprint(user_id: str, session_id: str | None, message: str, mode: str | None, version: str = "v1") -> str:
    value = {"user": user_id, "session": session_id or "", "message": message, "mode": mode or "", "version": version}
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest()


def period_bounds(period: str | None, now: datetime) -> tuple[str, datetime, datetime]:
    value = now.astimezone(timezone.utc).replace(tzinfo=None) if now.tzinfo else now
    name = (period or "MONTH").upper()
    if name in {"DAY", "DAILY"}:
        start = value.replace(hour=0, minute=0, second=0, microsecond=0)
        end = start + timedelta(days=1)
    else:
        start = value.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        end = (start.replace(day=28) + timedelta(days=4)).replace(day=1)
    return f"{name}:{start:%Y-%m-%dT%H:%M:%S}", start, end


def snapshot_hash(payload: dict) -> str:
    canonical = json.dumps(payload, sort_keys=True, separators=(",", ":"), default=str)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()
