"""Projection application and local feature checks for Recommendation."""

from __future__ import annotations

from datetime import datetime, timezone

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.core.problem import ProblemDetailException
from app.entities.entitlement import (
    EntitlementEventInbox,
    UserEntitlementProjection,
    UserFeatureEntitlement,
)
from app.services.quota_contract import snapshot_hash


FEATURE_AGENT_ADVANCED = "recommendation.agent.advanced"


def _naive_utc(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is not None:
        return value.astimezone(timezone.utc).replace(tzinfo=None)
    return value


def apply_snapshot(db: Session, event: dict, now: datetime | None = None) -> str:
    """Apply a full snapshot atomically.

    Returns ``applied``, ``duplicate`` or ``stale``.  Callers own the outer
    transaction; this makes it possible for RabbitMQ acknowledgement to happen
    only after commit.
    """
    now = _naive_utc(now or datetime.now(timezone.utc))
    payload = event.get("payload") if isinstance(event.get("payload"), dict) else event
    user_id = str(payload.get("userId") or payload.get("subjectId") or "").strip()
    if not user_id:
        raise ValueError("entitlement snapshot requires userId")
    version = int(payload.get("entitlementVersion") or payload.get("version") or 0)
    event_id = str(event.get("eventId") or payload.get("eventId") or snapshot_hash(event))

    inbox = db.get(EntitlementEventInbox, event_id)
    if inbox is not None:
        return "duplicate"
    parent = db.get(UserEntitlementProjection, user_id)
    if parent is not None and version <= int(parent.entitlement_version):
        db.add(EntitlementEventInbox(event_id=event_id, user_id=user_id, entitlement_version=version, received_at=now))
        return "stale"

    if parent is None:
        parent = UserEntitlementProjection(user_id=user_id, subject_type=str(payload.get("subjectType") or "USER"))
        db.add(parent)
    parent.subject_type = str(payload.get("subjectType") or parent.subject_type or "USER")
    parent.entitlement_version = version
    parent.access_status = str(payload.get("accessStatus") or "UNKNOWN").upper()
    parent.snapshot_hash = snapshot_hash(payload)
    # Commerce v1 names the lease ``reconcileBy`` and the access boundary
    # ``accessEffectiveUntil``. Accept the explicit projection names too so
    # future snapshots can be adopted without another compatibility branch.
    parent.freshness_lease_until = _naive_utc(_parse_time(
        payload.get("freshnessLeaseUntil") or payload.get("reconcileBy")
    ))
    parent.valid_until = _naive_utc(_parse_time(
        payload.get("validUntil") or payload.get("accessEffectiveUntil")
        or payload.get("currentPeriodEnd")
    ))
    parent.received_at = now
    parent.disabled = bool(payload.get("disabled", False))

    db.execute(delete(UserFeatureEntitlement).where(UserFeatureEntitlement.user_id == user_id))
    for feature in payload.get("features") or []:
        if not isinstance(feature, dict) or not (feature.get("featureKey") or feature.get("key")):
            continue
        value_type = str(feature.get("valueType") or feature.get("type") or "BOOLEAN").upper()
        raw_value = feature.get("value", feature.get("enabled"))
        quota_limit = feature.get("quotaLimit", feature.get("limit"))
        if quota_limit is None and value_type == "QUOTA":
            quota_limit = raw_value
        db.add(UserFeatureEntitlement(
            user_id=user_id,
            feature_key=str(feature.get("featureKey") or feature.get("key")),
            value_type=value_type,
            boolean_value=bool(raw_value if raw_value is not None else feature.get("enabled", False)),
            quota_limit=int(quota_limit) if quota_limit is not None else None,
            period=str(feature.get("period") or feature.get("resetPolicy"))
            if feature.get("period") is not None or feature.get("resetPolicy") is not None else None,
            period_start=_naive_utc(_parse_time(feature.get("periodStart"))),
            period_end=_naive_utc(_parse_time(feature.get("periodEnd"))),
            valid_until=_naive_utc(_parse_time(
                feature.get("validUntil") or feature.get("effectiveUntil")
                or feature.get("periodEnd")
            )),
            enabled=bool(feature.get("enabled", True)),
            entitlement_version=version,
        ))
    db.add(EntitlementEventInbox(event_id=event_id, user_id=user_id, entitlement_version=version, received_at=now))
    return "applied"


def is_projection_fresh(parent: UserEntitlementProjection | None, now: datetime | None = None) -> bool:
    if parent is None or parent.disabled:
        return False
    now = _naive_utc(now or datetime.now(timezone.utc))
    if parent.freshness_lease_until is not None and parent.freshness_lease_until <= now:
        return False
    if parent.valid_until is not None and parent.valid_until <= now:
        return False
    return True


def require_feature(db: Session, user_id: str, feature_key: str, now: datetime | None = None) -> UserFeatureEntitlement:
    parent = db.get(UserEntitlementProjection, user_id)
    if not is_projection_fresh(parent, now):
        raise ProblemDetailException(503, "ENTITLEMENT_PROJECTION_STALE", "Entitlement projection is stale", retryable=True)
    if parent is None or parent.access_status not in {"ACTIVE", "GRACE"}:
        raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "The requested feature is not entitled")
    feature = db.scalar(select(UserFeatureEntitlement).where(
        UserFeatureEntitlement.user_id == user_id,
        UserFeatureEntitlement.feature_key == feature_key,
    ))
    if feature is None or not feature.enabled:
        raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "The requested feature is not entitled")
    if feature.value_type == "QUOTA" and parent.access_status == "GRACE":
        # The v1 standard billing policy denies metered use during grace even
        # when the boolean projection remains readable.
        raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "Metered feature is unavailable during grace")
    expiry = _naive_utc(feature.valid_until)
    current = _naive_utc(now or datetime.now(timezone.utc))
    if expiry is not None and expiry <= current:
        raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "The requested feature is not entitled")
    if feature.value_type == "BOOLEAN" and not feature.boolean_value:
        raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "The requested feature is not entitled")
    return feature


def _parse_time(value) -> datetime | None:
    if not value:
        return None
    if isinstance(value, datetime):
        return value
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None
