"""AI credits: token usage priced per model, reserved before a turn and settled at what it really cost."""

import math
from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from sqlalchemy import and_, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.billing.entities import CreditCounter, CreditReservation, UserEntitlement, UserFeature
from app.billing.projection import aware
from app.billing.rehydration import EntitlementRehydrator, entitlement_rehydrator
from app.core.config import settings
from app.core.problem import ProblemDetailException


# the one CozX credit pool: learning, the Studio and background jobs all spend it
CREDIT_FEATURE = "assistant.credits"
TOKENS_PER_CREDIT = 1000


@dataclass(frozen=True)
class TokenWeights:
    """Cost of one token relative to one plain input token. Output is dearer: generation is the slow part."""

    input: float = 1.0
    output: float = 4.0


def weights_for(model_name: str) -> TokenWeights:
    configured = settings.LLM_MODEL_WEIGHTS.get(model_name)
    return TokenWeights(**configured) if configured else TokenWeights()


def credits_for(model_name: str, input_tokens: int, output_tokens: int) -> int:
    """ceil((input x w_in + output x w_out) / 1000); any model use costs at least one credit."""
    if input_tokens <= 0 and output_tokens <= 0:
        return 0
    weights = weights_for(model_name)
    weighted = input_tokens * weights.input + output_tokens * weights.output
    return max(1, math.ceil(weighted / TOKENS_PER_CREDIT))


def period_key(feature: UserFeature, now: datetime) -> str:
    start, end = aware(feature.period_start), aware(feature.period_end)
    if (feature.reset_policy or "").upper() == "SUBSCRIPTION_PERIOD" and start and end and end > start:
        return f"SP:{start:%Y-%m-%dT%H:%M:%S}"
    return f"CM:{now:%Y-%m}"


def period_end(feature: UserFeature, now: datetime) -> datetime:
    """When the period `period_key` names ends, so the panel can say when credits come back."""
    start, end = aware(feature.period_start), aware(feature.period_end)
    if (feature.reset_policy or "").upper() == "SUBSCRIPTION_PERIOD" and start and end and end > start:
        return end
    return datetime(now.year + now.month // 12, now.month % 12 + 1, 1, tzinfo=timezone.utc)


def _stale(entitlement: UserEntitlement | None, now: datetime) -> bool:
    """No projection yet, or its reconcileBy lease has run out."""
    return entitlement is None or bool(entitlement.reconcile_by and aware(entitlement.reconcile_by) <= now)


class CreditService:
    def __init__(self, db: AsyncSession, clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
                 feature_key: str = CREDIT_FEATURE, rehydrator: EntitlementRehydrator | None = entitlement_rehydrator):
        self.db = db
        self.clock = clock
        self.feature_key = feature_key
        self.rehydrator = rehydrator

    async def reserve(self, user_id: str, reservation_id: str, amount: int, commit: bool = True,
                      hold_for: timedelta | None = None) -> CreditReservation:
        """Hold up to one turn's cap; refuses once the period has nothing left.

        hold_for: how long the work may run before the sweeper frees the hold, when longer than a chat turn.
        """
        now = self.clock()
        feature = await self._entitled_feature(user_id, now)
        key = period_key(feature, now)
        counter = await self.db.get(CreditCounter, (user_id, self.feature_key, key), with_for_update=True)
        if counter is None:
            counter = CreditCounter(user_id=user_id, feature_key=self.feature_key, period_key=key, used=0, reserved=0)
            self.db.add(counter)
        if not feature.unlimited:
            remaining = (feature.quota_limit or 0) - counter.used - counter.reserved
            if remaining <= 0:
                raise ProblemDetailException(429, "QUOTA_EXCEEDED", "No AI credits left in this period")
            # The last credits still allow a turn; the settle below never goes past the limit.
            amount = min(amount, remaining)
        counter.reserved += amount
        reservation = CreditReservation(id=reservation_id, user_id=user_id, feature_key=self.feature_key,
                                        period_key=key, amount=amount, status="RESERVED", created_at=now,
                                        expires_at=now + hold_for if hold_for else None)
        self.db.add(reservation)
        if commit:
            await self.db.commit()
        return reservation

    async def settle(self, reservation_id: str, credits: int) -> int:
        """Charge what the turn used (at most what was held) and free the rest. Returns the charge."""
        reservation = await self.db.get(CreditReservation, reservation_id, with_for_update=True)
        if reservation is None or reservation.status != "RESERVED":
            return reservation.charged if reservation else 0
        counter = await self.db.get(
            CreditCounter, (reservation.user_id, reservation.feature_key, reservation.period_key), with_for_update=True
        )
        charge = max(0, min(credits, reservation.amount))
        counter.reserved -= reservation.amount
        counter.used += charge
        reservation.charged = charge
        reservation.status = "SETTLED" if charge else "RELEASED"
        reservation.settled_at = self.clock()
        await self.db.commit()
        return charge

    async def release_abandoned(self, older_than: timedelta, limit: int = 200) -> int:
        """Free holds of work that died with its process; its usage is unknown, so nothing is charged.

        A hold with its own expires_at waits for it; any other is abandoned once older than older_than.
        """
        now = self.clock()
        stale = (await self.db.scalars(
            select(CreditReservation)
            .where(CreditReservation.status == "RESERVED", or_(
                and_(CreditReservation.expires_at.is_(None), CreditReservation.created_at < now - older_than),
                CreditReservation.expires_at < now,
            ))
            .limit(limit)
        )).all()
        for reservation in stale:
            await self.settle(reservation.id, 0)
        return len(stale)

    async def remaining(self, user_id: str) -> dict:
        now = self.clock()
        feature = await self._entitled_feature(user_id, now)
        counter = await self.db.get(CreditCounter, (user_id, self.feature_key, period_key(feature, now)))
        used = counter.used if counter else 0
        return {"limit": None if feature.unlimited else feature.quota_limit, "used": used,
                "unlimited": feature.unlimited, "unit": feature.unit,
                "resetsAt": period_end(feature, now).isoformat()}

    async def _entitled_feature(self, user_id: str, now: datetime) -> UserFeature:
        entitlement = await self.db.get(UserEntitlement, user_id)
        if _stale(entitlement, now) and self.rehydrator and await self.rehydrator.rehydrate(self.db, user_id, now):
            entitlement = await self.db.get(UserEntitlement, user_id, populate_existing=True)
        if _stale(entitlement, now):
            raise ProblemDetailException(503, "ENTITLEMENT_PROJECTION_STALE",
                                         "Your plan details are being refreshed; try again shortly", retryable=True)
        if entitlement.access_status != "ACTIVE":
            # GRACE keeps reads but not metered use, as in the v1 billing policy.
            raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "AI credits are not available on this account")
        # populate_existing: a rehydration above may have replaced the rows in a session of its own
        feature = await self.db.scalar(select(UserFeature).where(
            UserFeature.user_id == user_id, UserFeature.feature_key == self.feature_key
        ).execution_options(populate_existing=True))
        if (
            feature is None
            or feature.value_type != "QUOTA"
            or (feature.effective_until and aware(feature.effective_until) <= now)
            or (not feature.unlimited and not feature.quota_limit)
        ):
            raise ProblemDetailException(403, "FEATURE_NOT_ENTITLED", "AI credits are not included in this plan")
        return feature
