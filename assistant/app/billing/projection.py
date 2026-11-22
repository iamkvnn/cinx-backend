"""Apply Commerce entitlement snapshots (commerce.subscription.entitlements.changed v1)."""

import logging
from datetime import datetime, timezone

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.billing.entities import EntitlementInbox, UserEntitlement, UserFeature


logger = logging.getLogger(__name__)


class InvalidSnapshot(ValueError):
    """The payload cannot be applied; redelivery would not help."""


def parse_time(value) -> datetime | None:
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError as exc:
        raise InvalidSnapshot(f"Invalid timestamp {value!r}") from exc
    # Some Java publishers send LocalDateTime without an offset (x-known-runtime-drift): read it as UTC.
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def aware(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


async def apply_snapshot(db: AsyncSession, payload: dict, now: datetime, reconciliation: bool = False) -> str:
    """Replace the user's projection with a newer full snapshot. Returns applied, duplicate, stale, renewed or conflict.

    A broker event is claimed by its eventId. A reconciliation answer (Commerce's internal endpoint) has none; at the
    stored version it may only extend the reconcileBy lease, and only when its content matches what is stored.
    The caller commits; acknowledging the message only after that commit makes delivery at-least-once safe.
    """
    try:
        event_id = None if reconciliation else str(payload["eventId"])
        user_id = str(payload["userId"])
        version = int(payload["entitlementVersion"])
        features = list(payload["features"])
    except (KeyError, TypeError, ValueError) as exc:
        raise InvalidSnapshot(f"Missing or invalid field: {exc}") from exc
    if event_id == "" or not user_id:
        raise InvalidSnapshot("eventId and userId are required")

    if event_id is not None and await db.get(EntitlementInbox, event_id) is not None:
        return "duplicate"
    # Locked: the consumer and a reconciliation may race, and the older version must not land last.
    current = await db.get(UserEntitlement, user_id, with_for_update=True)
    if current is not None and version <= current.entitlement_version:
        if reconciliation:
            return await _renew(db, current, version, payload, now)
        db.add(EntitlementInbox(event_id=event_id, user_id=user_id, entitlement_version=version,
                                outcome="stale", received_at=now))
        return "stale"

    if current is None:
        current = UserEntitlement(user_id=user_id)
        db.add(current)
    current.entitlement_version = version
    current.access_status = _access_status(payload)
    current.reconcile_by = parse_time(payload.get("reconcileBy"))
    current.access_effective_until = parse_time(payload.get("accessEffectiveUntil"))
    current.received_at = now

    await db.execute(delete(UserFeature).where(UserFeature.user_id == user_id))
    db.add_all(_feature_rows(user_id, features))
    if event_id is not None:
        db.add(EntitlementInbox(event_id=event_id, user_id=user_id, entitlement_version=version,
                                outcome="applied", received_at=now))
    return "applied"


async def _renew(db: AsyncSession, current: UserEntitlement, version: int, payload: dict, now: datetime) -> str:
    if version < current.entitlement_version:
        return "stale"
    stored = (await db.scalars(select(UserFeature).where(UserFeature.user_id == current.user_id))).all()
    answered = _feature_rows(current.user_id, list(payload["features"]))
    if _canonical(current.access_status, stored) != _canonical(_access_status(payload), answered):
        logger.warning("Commerce answered entitlement version %s of user %s with other content; projection kept",
                       version, current.user_id)
        return "conflict"
    lease = parse_time(payload.get("reconcileBy"))
    if lease is None or (current.reconcile_by is not None and lease > aware(current.reconcile_by)):
        current.reconcile_by = lease
    current.received_at = now
    return "renewed"


def _access_status(payload: dict) -> str:
    return str(payload.get("accessStatus") or "INACTIVE").upper()


def _feature_rows(user_id: str, features: list) -> list[UserFeature]:
    rows = []
    for feature in features:
        if not isinstance(feature, dict) or not feature.get("key"):
            raise InvalidSnapshot("Every feature needs a key")
        rows.append(UserFeature(
            user_id=user_id,
            feature_key=str(feature["key"]),
            value_type=str(feature.get("type") or "BOOLEAN").upper(),
            enabled=bool(feature.get("enabled", False)),
            quota_limit=int(feature["limit"]) if feature.get("limit") is not None else None,
            unlimited=bool(feature.get("unlimited", False)),
            unit=feature.get("unit"),
            reset_policy=feature.get("resetPolicy"),
            period_start=parse_time(feature.get("periodStart")),
            period_end=parse_time(feature.get("periodEnd")),
            effective_until=parse_time(feature.get("effectiveUntil")),
        ))
    return rows


def _canonical(access_status: str, features) -> tuple:
    """What a reconciliation answer must repeat to count as the stored version."""
    return access_status, sorted(
        ((f.feature_key, f.value_type, bool(f.enabled), f.quota_limit, bool(f.unlimited), f.unit, f.reset_policy,
          aware(f.period_start), aware(f.period_end), aware(f.effective_until)) for f in features),
        key=lambda row: row[0],
    )
