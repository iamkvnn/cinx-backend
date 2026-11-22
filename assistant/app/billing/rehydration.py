"""Rehydrate a missing or expired entitlement projection from Commerce's reconciliation endpoint.

Commerce publishes commerce.subscription.entitlements.changed only when a subscription changes, so a user who
onboarded before the assistant consumed it has no projection, and a snapshot's reconcileBy lease runs out without a
new event. Either way the assistant asks Commerce for the effective snapshot and applies it as the consumer would.
"""

import logging
import time
from collections.abc import Awaitable, Callable
from datetime import datetime
from urllib.parse import quote

import httpx
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.billing.projection import InvalidSnapshot, apply_snapshot
from app.core.config import settings
from app.core.logging import outbound_trace_headers
from app.security.service_token import service_access_token


logger = logging.getLogger(__name__)
COMMERCE_INTERNAL_AUDIENCE = "cinx-commerce-internal"
# past this many remembered users, the ones whose cooldown is over are forgotten
COOLDOWN_PRUNE_SIZE = 10_000

# user id -> Commerce's effective snapshot; swapped for a fake in tests
FetchSnapshot = Callable[[str], Awaitable[dict]]


class SnapshotUnavailable(Exception):
    """Commerce could not answer now; the caller keeps failing closed."""


async def fetch_from_commerce(user_id: str, transport: httpx.AsyncBaseTransport | None = None) -> dict:
    """GET /internal/entitlements/users/{userId} with the assistant's service token."""
    try:
        token = await service_access_token(COMMERCE_INTERNAL_AUDIENCE)
    except (httpx.HTTPError, KeyError) as exc:
        raise SnapshotUnavailable("no service token for commerce") from exc
    async with httpx.AsyncClient(base_url=settings.COMMERCE_SERVICE_BASE_URL, transport=transport,
                                 timeout=settings.COMMERCE_INTERNAL_TIMEOUT_SECONDS) as client:
        try:
            response = await client.get(f"/internal/entitlements/users/{quote(user_id, safe='')}",
                                        headers={"Authorization": f"Bearer {token}", "Accept": "application/json",
                                                 **outbound_trace_headers()})
        except httpx.HTTPError as exc:
            raise SnapshotUnavailable("commerce is unreachable") from exc
    if response.status_code >= 400:
        raise SnapshotUnavailable(f"commerce answered {response.status_code}")
    try:
        body = response.json()
    except ValueError as exc:
        raise SnapshotUnavailable("commerce answered no JSON") from exc
    snapshot = body.get("data") if isinstance(body, dict) else None
    if not isinstance(snapshot, dict):
        raise SnapshotUnavailable("commerce answered no snapshot")
    return snapshot


class EntitlementRehydrator:
    """Asks Commerce at most once per user per cooldown, so neither an unreachable Commerce nor a user without a
    plan turns every request into a call; the stored snapshot's own lease covers the time after a good answer."""

    def __init__(self, fetch: FetchSnapshot = fetch_from_commerce, cooldown_seconds: float | None = None,
                 monotonic: Callable[[], float] = time.monotonic):
        self.fetch = fetch
        self.cooldown = settings.ENTITLEMENT_REHYDRATE_COOLDOWN_SECONDS if cooldown_seconds is None else cooldown_seconds
        self.monotonic = monotonic
        self._not_before: dict[str, float] = {}

    async def rehydrate(self, db: AsyncSession, user_id: str, now: datetime) -> bool:
        """True when the stored projection may have changed and is worth reading again.

        The snapshot is committed in a session of its own, like a consumed event, so it outlives a caller that
        rolls back (a turn refused for want of credits).
        """
        moment = self.monotonic()
        if self._not_before.get(user_id, 0.0) > moment:
            return False
        if len(self._not_before) >= COOLDOWN_PRUNE_SIZE:
            self._not_before = {key: until for key, until in self._not_before.items() if until > moment}
        self._not_before[user_id] = moment + self.cooldown
        try:
            snapshot = await self.fetch(user_id)
        except SnapshotUnavailable as exc:
            logger.warning("Entitlement snapshot of user %s unavailable: %s", user_id, exc)
            return False
        if str(snapshot.get("userId") or "") != user_id:
            logger.warning("Commerce answered an entitlement snapshot of another user for %s", user_id)
            return False
        async with AsyncSession(db.bind, expire_on_commit=False) as own:
            try:
                outcome = await apply_snapshot(own, snapshot, now, reconciliation=True)
                await own.commit()
            except (InvalidSnapshot, TypeError, ValueError) as exc:
                logger.warning("Entitlement snapshot of user %s cannot apply: %s", user_id, exc)
                return False
            except IntegrityError:
                # the consumer or another request stored this user first
                await own.rollback()
                return True
        return outcome in ("applied", "renewed")


entitlement_rehydrator = EntitlementRehydrator()
